package com.example.threadopsy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

sealed class AuthState {
    object Unauthenticated : AuthState()
    object Guest : AuthState()
    data class Authenticated(val userId: String, val email: String) : AuthState()
}

class ThreadopsyViewModel : ViewModel() {

    private val repository = ClosetRepository()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // Tracks whether the user is Signed Out, Guest, or Authenticated
    private val _authState = MutableStateFlow<AuthState>(
        auth.currentUser?.let { user ->
            AuthState.Authenticated(user.uid, user.email ?: "User")
        } ?: AuthState.Unauthenticated
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Automatically switches closet stream based on the logged-in user (empty for Guests)
    @OptIn(ExperimentalCoroutinesApi::class)
    val closetItems: StateFlow<List<ClothingItem>> = _authState
        .flatMapLatest { state ->
            when (state) {
                is AuthState.Authenticated -> repository.getClosetStream(state.userId)
                else -> flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentAutopsyItem = MutableStateFlow<ClothingItem?>(null)
    val currentAutopsyItem: StateFlow<ClothingItem?> = _currentAutopsyItem.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private var sampleIndex = 0

    init {
        loadNextSampleTag()
    }

    // --- Authentication Actions ---

    fun signIn(email: String, password: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.length < 6) {
            _statusMessage.value = "Enter a valid email and a 6+ character password."
            return
        }

        _isAuthLoading.value = true
        auth.signInWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                _isAuthLoading.value = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userEmail = user.email ?: cleanEmail
                        repository.saveUserProfile(user.uid, userEmail)
                        _authState.value = AuthState.Authenticated(user.uid, userEmail)
                        _statusMessage.value = "Welcome back!"
                    }
                } else {
                    _statusMessage.value = task.exception?.localizedMessage ?: "Sign in failed."
                }
            }
    }

    fun signUp(email: String, password: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || password.length < 6) {
            _statusMessage.value = "Use a valid email and at least 6 characters for password."
            return
        }

        _isAuthLoading.value = true
        auth.createUserWithEmailAndPassword(cleanEmail, password)
            .addOnCompleteListener { task ->
                _isAuthLoading.value = false
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        val userEmail = user.email ?: cleanEmail
                        repository.saveUserProfile(user.uid, userEmail)
                        _authState.value = AuthState.Authenticated(user.uid, userEmail)
                        _statusMessage.value = "Account created and synced!"
                    }
                } else {
                    _statusMessage.value = task.exception?.localizedMessage ?: "Sign up failed."
                }
            }
    }

    fun continueAsGuest() {
        _authState.value = AuthState.Guest
        _statusMessage.value = "Browsing as Guest (Closet is locked)"
    }

    fun signOutOrExitGuest() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }

    // --- Scanner & Closet Actions ---

    fun onTagTextRecognized(rawText: String, customName: String = "", price: Double = 500.0) {
        if (rawText.isBlank()) return
        _currentAutopsyItem.value = ForensicParser.analyzeTag(
            rawText = rawText,
            customName = customName,
            price = price
        )
    }

    fun loadNextSampleTag() {
        val sampleText = ForensicParser.sampleTestTags[sampleIndex % ForensicParser.sampleTestTags.size]
        sampleIndex++
        onTagTextRecognized(sampleText)
    }

    fun updateCurrentItemDetails(newName: String, newPrice: Double) {
        val current = _currentAutopsyItem.value ?: return
        _currentAutopsyItem.value = current.copy(
            garmentName = newName.ifBlank { current.garmentName },
            purchasePrice = newPrice.coerceAtLeast(0.0)
        )
    }
    /**
     * Adds or removes a decoded laundry symbol instruction from the current garment's Care Guide.
     */
    fun toggleSymbolTip(symbolTip: String) {
        val current = _currentAutopsyItem.value ?: return
        if (current.fabricBreakdown == "---") return

        val updatedProtocols = if (current.washProtocol.contains(symbolTip)) {
            current.washProtocol - symbolTip
        } else {
            current.washProtocol + symbolTip
        }
        _currentAutopsyItem.value = current.copy(washProtocol = updatedProtocols)
    }
    fun selectClosetItemForInspection(item: ClothingItem) {
        _currentAutopsyItem.value = item
    }

    fun saveCurrentToCloset() {
        val currentState = _authState.value
        if (currentState !is AuthState.Authenticated) {
            _statusMessage.value = "Please sign in to save items to your Closet."
            return
        }

        val itemToSave = _currentAutopsyItem.value ?: return
        repository.saveGarment(currentState.userId, itemToSave) { _, message ->
            _statusMessage.value = message
        }
    }

    fun logWear(item: ClothingItem) {
        val currentState = _authState.value as? AuthState.Authenticated ?: return
        repository.incrementWearCount(currentState.userId, item) { errorMsg ->
            _statusMessage.value = errorMsg
        }
        if (_currentAutopsyItem.value?.id == item.id) {
            _currentAutopsyItem.value = item.copy(timesWorn = item.timesWorn + 1)
        }
    }

    fun deleteFromCloset(itemId: String) {
        val currentState = _authState.value as? AuthState.Authenticated ?: return
        repository.deleteGarment(currentState.userId, itemId) { errorMsg ->
            _statusMessage.value = errorMsg
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }
}