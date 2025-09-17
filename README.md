# Android App

A modern Android application built with Jetpack Compose that demonstrates secure authentication patterns, network monitoring, and comprehensive testing strategies.



## ✨ Features

### Authentication & Security
- **Secure Login System**: Email-based authentication with demo credentials
- **Account Protection**: Automatic lockout after 3 failed attempts (30-second cooldown)
- **Session Management**: "Remember Me" functionality with secure token storage
- **Real-time Validation**: Input validation for email format and password strength
- **Offline Detection**: Graceful handling of network connectivity issues

### User Experience
- **Modern UI**: Built with Jetpack Compose and Material Design 3
- **Responsive Design**: Edge-to-edge display with proper padding
- **Loading States**: Visual feedback during authentication
- **Error Handling**: Clear error messages with attempt counters

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture Pattern**: MVVM (Model-View-ViewModel)
- **Dependency Injection**: Manual DI (ready for Dagger/Hilt)
- **Async Operations**: Kotlin Coroutines + Flow
- **Testing**: JUnit 4, Mockito, Kotlin Test

### Project Structure
```
app/src/main/java/com/emirates/loyaltypoints/
├── MainActivity.kt                          # Entry point and navigation logic
├── data/
│   ├── repository/
│   │   └── AuthRepository.kt               # Authentication business logic
│   └── network/
│       └── NetworkMonitor.kt               # Network connectivity monitoring
├── presentation/
│   ├── login/
│   │   ├── LoginViewModel.kt               # Login state management
│   │   ├── LoginUiState.kt                 # UI state definitions
│   │   └── LoginScreen.kt                  # Login UI components
│   └── home/
│       └── HomeScreen.kt                   # Post-login dashboard
└── ui/theme/                               # Material Design theme
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## 🔧 Implementation Details

### Authentication Repository (`AuthRepository.kt`)
The `AuthRepositoryImpl` class provides:
- **Demo Credentials**: `user@emirates.com` / `password123`
- **Token Management**: Secure storage using SharedPreferences
- **Input Validation**: Empty field checks and credential validation
- **Simulated Network Delay**: 1.5-second delay for realistic UX

**Key Methods:**
- `login(credentials)`: Authenticates user with validation
- `saveAuthToken(token)`: Stores JWT token securely
- `getAuthToken()`: Retrieves stored token
- `clearAuthToken()`: Removes token on logout
- `saveRememberMe(remember)`: Manages persistence preference

### Login ViewModel (`LoginViewModel.kt`)
Comprehensive state management with:
- **Real-time Validation**: Email format and password length checks
- **Lockout Mechanism**: 3-attempt limit with 30-second countdown
- **Network Awareness**: Automatic offline detection and handling
- **Unidirectional Data Flow**: Events → State updates → UI reactions

**Key Features:**
- Input validation: Email regex and 6+ character passwords
- Account lockout: Progressive failure counting with timer
- Auto-login: Checks for existing valid sessions
- Error handling: User-friendly messages with attempt counters

### Network Monitor (`NetworkMonitor.kt`)
Real-time connectivity tracking:
- **Reactive Monitoring**: Flow-based network state changes
- **Multiple Transports**: WiFi and cellular network support
- **Internet Capability**: Verifies actual internet access, not just connection
- **Callback Management**: Proper registration and cleanup

## 🧪 Testing Strategy

### Unit Tests Coverage
The project includes comprehensive unit tests covering:

#### AuthRepository Tests (`AuthRepositoryTest.kt`)
- ✅ Valid credential authentication
- ✅ Invalid username/password handling
- ✅ Empty field validation
- ✅ Token storage and retrieval
- ✅ Remember Me preference management
- ✅ SharedPreferences integration

#### LoginViewModel Tests (`LoginViewModelTest.kt`)
- ✅ Input validation and button state management
- ✅ Successful login navigation
- ✅ Failure count and lockout mechanism
- ✅ Timer countdown functionality
- ✅ Offline state handling
- ✅ Remember Me token persistence
- ✅ Auto-login with existing tokens
- ✅ Error message lifecycle

#### Network Monitor Tests (`NetworkMonitorTest.kt`)
- ✅ Connectivity state tracking
- ✅ Network callback handling
- ✅ Internet capability verification

### Testing Technologies
- **JUnit 4**: Core testing framework
- **Mockito**: Mock object creation and verification
- **Kotlin Test**: Kotlin-specific assertions
- **Coroutines Test**: Async operation testing with `runTest`
- **StandardTestDispatcher**: Deterministic coroutine execution

## 🚀 Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (minimum)
- Kotlin 1.9+

### Setup Instructions
1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd LoyaltyPoints
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - Select "Open an existing project"
   - Navigate to the project directory

3. **Build the project**
   ```bash
   ./gradlew build
   ```

4. **Run tests**
   ```bash
   ./gradlew test
   ```

5. **Install on device/emulator**
   ```bash
   ./gradlew installDebug
   ```

### Demo Credentials
- **Username**: `user@emirates.com`
- **Password**: `password123`

## 🧪 Running Tests

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests="*AuthRepositoryTest*"

# Run with coverage
./gradlew testDebugUnitTestCoverageReport
```

### Instrumented Tests
```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run on specific device
./gradlew connectedDebugAndroidTest
```

## 🔒 Security Features

### Authentication Security
- **Input Sanitization**: Username trimming and validation
- **Rate Limiting**: Progressive lockout mechanism
- **Session Management**: Secure token storage
- **Offline Protection**: Prevents authentication without network

### Data Protection
- **SharedPreferences**: Secure local storage
- **Token Isolation**: Separate storage keys
- **Auto-cleanup**: Token removal on logout

## 🎨 UI/UX Design

### Material Design 3
- **Theme System**: Light/dark theme support ready
- **Typography**: Consistent text styles
- **Color Scheme**: Emirates brand colors
- **Component Library**: Material 3 components

### Accessibility
- **Screen Reader Support**: Semantic content descriptions
- **Touch Targets**: Minimum 48dp touch areas
- **Contrast**: WCAG compliant color ratios

## 📝 Code Quality

### Architecture Principles
- **Single Responsibility**: Each class has one clear purpose
- **Dependency Inversion**: Interface-based abstractions
- **Testability**: Constructor injection for easy mocking
- **Immutability**: Data classes and StateFlow for state

### Code Standards
- **Comprehensive Documentation**: KDoc comments on all public APIs
- **Error Handling**: Graceful failure management
- **Resource Management**: Proper cleanup and lifecycle handling
- **Performance**: Efficient state updates and memory usage

## 🔄 Future Enhancements

### Planned Features
- **Biometric Authentication**: Fingerprint/face unlock
- **Multi-factor Authentication**: SMS/email verification
- **Points Dashboard**: Interactive loyalty points display
- **Transaction History**: Detailed points earning/spending
- **Offline Support**: Local data caching

### Technical Improvements
- **Dependency Injection**: Migration to Dagger/Hilt
- **Database**: Room integration for local storage
- **API Integration**: Real backend service connection
- **CI/CD**: Automated testing and deployment

