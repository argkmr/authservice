# Auth Service Notes

### Full Project Architecture & Flow

![Google Login Flow](assets/authservice-overview.png)

---

## Basic Login Flow

```
User → Frontend → Login Controller → CustomUserDetailsService → UserRepository
                                   ↓
User ← Frontend ← JWT Response ← JwtUtils.generateToken() ← UserDetails
```

---

## Google OAuth2 Flow

### 1. INITIATION

```
User → Frontend → SecurityConfig → Google OAuth2 Server
                                           ↓
```

### 2. CALLBACK & PROCESSING

```
OAuth2AuthenticationSuccessHandler ← Google (callback)
                    ↓
   ┌─ Save user to GoogleUsersRepository
   ├─ Get tokens from OAuth2AuthorizedClientService  
   ├─ Load UserDetails via CustomGoogleUserDetailsService
   ├─ Generate JWT via JwtUtils
   ├─ Store JWT code in CodeStore
   ├─ Store Gmail tokens in CodeStore
   └─ Redirect to Frontend with retrieval codes
```

### 3. TOKEN RETRIEVAL

```
Frontend → Google Controller (/google/jwt-token) → CodeStore → JWT
Frontend → Google Controller (/google/gmail-tokens) → CodeStore → Gmail Tokens
```

---

## Important Classes And Their Functionalities

### SecurityConfig.java

**Purpose:** Configures Spring Security for the application, supporting:
- Basic Authentication (stateless)
- Optional Google OAuth2 authentication
- CORS setup for frontend integration
- Custom authorization rules

#### Annotations
- `@Configuration`: Marks this class as a Spring configuration class.
- `@EnableWebSecurity`: Enables Spring Security's web security support.
- `@Bean`: Marks methods that produce Spring-managed beans.

#### Injected Dependencies
**OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler**
- Custom handler triggered after successful Google OAuth2 login.
- Typically used for post-login logic such as JWT generation or redirection.

#### SecurityFilterChain Bean
Configures the HTTP security pipeline.

##### 1. CORS Configuration

```java
.cors(cors -> cors.configurationSource(request -> {
    CorsConfiguration c = new CorsConfiguration();
    c.setAllowedOrigins(List.of("http://localhost:3000"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setAllowCredentials(true);
    return c;
}))
```

- Allows cross-origin requests from http://localhost:3000 (frontend origin).
- Permits all headers and standard HTTP methods.
- Allows credentials (cookies/authorization headers) in cross-origin requests.

##### 2. CSRF Disabled

```java
.csrf(AbstractHttpConfigurer::disable)
```

Disables CSRF protection.

Common when:
- Backend is stateless (e.g., using JWT)
- API is accessed by trusted frontend or external clients
- For stateful sessions, this should usually remain enabled.

##### 3. Authorization Rules

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**", "/google/**").permitAll()
    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
    .anyRequest().authenticated()
)
```

**Permit All:**
- `/api/auth/**`: All authentication-related endpoints (e.g., login, registration)
- `/google/**`: All Google OAuth2-related endpoints
- All HTTP OPTIONS requests (CORS preflight checks)

**Authenticated:**
- All other endpoints require authentication.

##### 4. Session Management

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
)
```

**IF_REQUIRED:**
- A session is created only if needed (e.g., during OAuth2 login flow).
- For full stateless JWT-based authentication, STATELESS is typically preferred.
- This setting allows partial statefulness for Google login while keeping other flows stateless.

##### 5. OAuth2 Login Configuration

```java
.oauth2Login(oauth2 -> oauth2
    .successHandler(oAuth2AuthenticationSuccessHandler)
)
```

- Enables OAuth2 login with Google (or other providers).
- Custom successHandler is executed after successful OAuth2 login:
- Typically used to generate JWT or store user info.

##### 6. HTTP Basic Authentication

```java
.httpBasic(Customizer.withDefaults())
```

- Enables HTTP Basic Authentication for endpoints requiring authentication.
- Common in API testing or backend-to-backend communication.
- Works alongside OAuth2 login.

#### PasswordEncoder Bean

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

- Provides a password encoder for hashing and verifying passwords.
- Uses BCrypt, a strong one-way hashing algorithm.

#### Key Points in Context
- **Basic Auth:** Handled via `.httpBasic()` and `/api/auth/**` endpoints.
- **Google Auth:** Handled via `.oauth2Login()` with a custom success handler.
- **Statelessness:**
  - Basic Auth is fully stateless.
  - Google OAuth2 is partially stateful due to IF_REQUIRED session policy.
- **CORS:** Explicitly allows frontend communication.
- **Security Implication:** With CSRF disabled and session creation allowed, ensure that any session-based endpoints are not exposed to CSRF risks.

---

### JwtUtils.java

#### Class Overview
**Purpose:** Utility class for generating, parsing, and validating JWT tokens in the authentication system.

**Scope:** Works with Spring Security UserDetails and signed tokens using HS256 algorithm.

#### Annotations
- `@Component`: Marks this class as a Spring Bean for dependency injection.
- `@Value`: Injects values from application.properties or application.yml:
  - `jwt.secret`: Base64-encoded secret key for signing JWT.
  - `jwt.expiration`: Token expiration time in milliseconds.

#### Injected Properties
- **secretKey (String)**: Base64-encoded secret used for signing.
- **expirationInMs (long)**: Token validity period.

#### Methods

##### 1. generateToken(UserDetails userDetails)
**Purpose:** Creates a signed JWT for the authenticated user.

**Steps:**
- Calls `getSigningKey()` to obtain the HMAC signing key.
- Calculates expiration date via `getExpiratonDate()`.
- Extracts user roles from `userDetails.getAuthorities()`.
- Builds JWT:
  - `setSubject`: Stores the username as the subject.
  - `claim("roles", roles)`: Adds roles array as a custom claim.
  - `setIssuedAt`: Current timestamp.
  - `setExpiration`: Expiration date.
  - `signWith`: Signs the token using HS256 and the secret key.
- Returns the compact (string) representation of the JWT.

##### 2. getExpiratonDate()
Returns Date object for token expiration: `currentTimeMillis + expirationInMs`

##### 3. extractUsername(String token)
**Purpose:** Retrieves username (subject) from a token.

**Steps:**
- Obtains signing key via `getSigningKey()`.
- Parses the JWT and extracts Claims object.
- Returns `claims.getSubject()` (username).

##### 4. getSigningKey()
- Decodes the Base64-encoded secretKey.
- Uses `Keys.hmacShaKeyFor()` to create a Key suitable for HMAC signing.

#### Key Points
- **Algorithm:** HS256 (HMAC with SHA-256).
- **Secret:** Must be Base64-encoded; length should be sufficient for the chosen algorithm.
- **Custom Claims:** Stores roles in "roles" claim.
- **Expiration Control:** Defined in application configuration, not hardcoded.
- **Security:**
  - Secret must be kept secure.
  - Tokens should be transmitted over HTTPS.
  - Expired tokens will be rejected by parseClaimsJws.

---

### CustomUserDetailsService.java

#### Class Overview
**Purpose:** Custom implementation of Spring Security's UserDetailsService to load user data from the database.

**Role in Flow:**
- Required by Spring Security for authentication.
- Supplies UserDetails object used in authentication and JWT generation (generateToken() in JwtUtilsneeds it).
- Scope: Looks up users by either username or email.

#### Annotations
- `@Service`: Marks this class as a Spring service bean, allowing Spring to detect and inject it.
- `@Autowired`: Injects UserRepository dependency.

#### Injected Dependencies
- **UserRepository**: Interface to fetch Users entity from persistence layer.

#### Methods

##### 1. loadUserByUsername(String usernameOrEmail)
**Purpose:** Loads a user from the database and returns a UserDetails object for Spring Security.

**Steps:**
- Attempts to find a user by username using `userRepository.getByUsername(usernameOrEmail)`.
- If not found, attempts to find by email using `userRepository.getByEmail(usernameOrEmail)`.
- If still not found, throws `UsernameNotFoundException` with an error message.
- Converts Users entity into Spring Security's built-in User object:
  - `.username(user.getUsername())` → Sets the username.
  - `.password(user.getPassword())` → Sets hashed password.
  - `.roles(user.getRole().getRoleName().name())` → Assigns roles based on user's role entity.

#### Why This is Important
**Integration with Spring Security:**
- Spring Security uses UserDetailsService during authentication to retrieve user credentials and authorities.
- This method is called automatically when a login request is processed.

**Required for JWT:**
- The `generateToken(UserDetails)` method in JwtUtils depends on UserDetails to:
  - Get the username (`getUsername()`).
  - Get user roles (`getAuthorities()`).
- Without this service, the system wouldn't be able to supply the authenticated UserDetails to JWT generation.

**Flexibility:**
- Allows login with either username or email without separate endpoints.

**Security:**
- Ensures passwords are stored in hashed form and passed securely to the authentication provider.
- Prevents leaking entity details by mapping only essential data to UserDetails.

---

### CustomGoogleUserDetailsService.java

#### Class Overview
**Purpose:** Custom implementation of Spring Security's UserDetailsService for Google OAuth2-authenticated users.

**Role in Flow:**
- Used when authentication is done via Google Sign-In instead of basic username/password.
- Supplies UserDetails object for security context population and potential JWT generation for Google users.
- Scope: Looks up Google users in the database by email.

#### Annotations
- `@Service`: Marks this class as a Spring service component for dependency injection.
- `@Autowired`: Injects GoogleUsersRepository dependency.

#### Injected Dependencies
- **GoogleUsersRepository**: Repository interface for retrieving GoogleUsers entity from persistence layer.

#### Methods

##### 1. loadUserByUsername(String email)
**Purpose:** Loads a Google user's data from the database using their email and returns a UserDetails object.

**Steps:**
- Uses `googleUsersRepository.getByEmail(email)` to retrieve GoogleUsers entity.
- If the user does not exist, throws `UsernameNotFoundException`.
- Maps the GoogleUsers entity to Spring Security's User object:
  - `.username(googleUser.getUsername())` → Sets username field (can be different from email if stored separately).
  - `.roles(googleUser.getRole())` → Sets roles assigned to the Google user.
  - `.password("")` → Password is set to empty string because Google OAuth2 authentication doesn't require stored passwords.

#### Why This is Important
**OAuth2 Integration:**
- Provides a way for Spring Security to integrate Google-authenticated users into the same SecurityContextused by basic auth users.
- Even though password is not used, Spring Security still requires a UserDetails object.

**JWT Generation:**
- If the system issues JWTs to Google users, `generateToken(UserDetails)` in JwtUtils will use this service to obtain:
  - Username for the token's subject.
  - Roles for authorization claims.

**Consistency Across Auth Methods:**
- Keeps the authentication process uniform whether login is via Basic Auth or Google OAuth2.

**Security:**
- Ensures Google accounts are mapped to roles in the system before access is granted.
- No password handling, reducing the attack surface for credential theft.

---

### CodeStore.java

#### Class Overview
**Purpose:** Temporary in-memory store for mapping short-lived authorization codes to tokens.

**Role in Flow:**
- Specifically used in Google OAuth2 integration.
- Acts as a bridge between the authorization code returned from Google and the JWT (or other token) generated by the system.
- Ensures a secure one-time retrieval process for exchanging codes with tokens.
- Scope: Holds data in memory only; not persistent across application restarts.

#### Annotations
- `@Component`: Registers the class as a Spring Bean for dependency injection, making it easily accessible in other components like OAuth2 handlers or controllers.

#### Internal Data Structures

**codeTokenMap:**
- `Map<String, String>` storing mappings of `"<type>:<code>"` → token.
- Stores which token corresponds to which authorization code.

**expiryMap:**
- `Map<String, Long>` storing the expiry timestamp for each code.
- Enforces time-limited validity (set to 60 seconds here).

**Both are ConcurrentHashMap:**
- Thread-safe for concurrent read/write in a multi-threaded web application environment.
- Prevents data corruption in concurrent access scenarios.

#### Methods

##### 1. saveCode(String type, String code, String token)
**Purpose:** Saves the mapping between the authorization code and the token, with a 60-second expiry.

**Steps:**
- Constructs a unique key by concatenating type and code.
- Stores the token in codeTokenMap under that key.
- Sets expiry timestamp (`System.currentTimeMillis() + 60000`) in expiryMap.

##### 2. getToken(String type, String code)
**Purpose:** Retrieves and removes the token associated with a given authorization code, if it has not expired.

**Steps:**
- Constructs the same key format (`type:code`).
- Checks if:
  - Key exists in expiryMap.
  - Current time is before expiry.
- If valid:
  - Removes the entry from both maps (one-time use).
  - Returns the associated token.
- If invalid or expired:
  - Returns null.

#### Importance in the Architecture
**Security:**
- Enforces short-lived, one-time use for authorization codes.
- Mitigates replay attacks where an attacker might try to reuse a stolen code.

**Google OAuth2 Integration:**
- After Google redirects the user back with an authorization code, the system stores a temporary mapping from that code to the generated JWT (or another token).
- The frontend then exchanges that code for the token securely.

**Stateless Authentication Support:**
- This component is necessary to bridge the gap between Google's temporary auth codes and the system's stateless JWT-based authentication without creating a persistent session.

**Scalability Consideration:**
- Works fine for single-instance deployments.
- For multiple server instances, this in-memory store would need to be replaced by a distributed store (e.g., Redis) to ensure code availability across nodes.

---

### OAuth2AuthenticationSuccessHandler.java

**Purpose:** This handler is triggered when a Google OAuth2 login succeeds. It's responsible for:
- Saving/creating the Google user in your database
- Generating JWT for your backend authentication
- Storing temporary "retrieval codes" in CodeStore for both JWT and Gmail tokens
- Redirecting the user to your configured frontend URL with those codes

#### Key Annotations
- `@Component` → Makes it a Spring-managed bean so Spring Security can call it automatically.
- `@RequiredArgsConstructor` → Generates a constructor for final fields.
- `@Autowired` → Injects repositories, services, and utilities.

#### Injected Dependencies
- **GoogleUsersRepository** → DB access for Google-based users.
- **CodeStore** → Secure temporary storage for JWT codes and Gmail tokens.
- **JwtUtils** → Generates JWT for your system.
- **CustomGoogleUserDetailsService** → Loads Google user details for token generation.
- **OAuth2AuthorizedClientService** → Retrieves Google access & refresh tokens.
- **_redirectUrl (from properties)** → Where to send the user after success.

#### Flow Inside onAuthenticationSuccess

##### Extract Google User Info
- From OAuth2User principal.
- Get email, derive username.

##### Save User if New
- If email doesn't exist in GoogleUsersRepository, create and save a new user.

##### Load OAuth2 Client Tokens
- Use authorizedClientService to get the access token and optional refresh token.

##### Generate JWT for Backend
- Use CustomGoogleUserDetailsService to load the user details.
- Call `jwtUtils.generateToken(...)` to get a JWT.
- Store JWT in CodeStore with a random retrieval code (jwtRetrievalCode).

##### Store Gmail Tokens
- Combine access token, refresh token, and username into a single string with separators (`_&&_`).
- Store in CodeStore with a different retrieval code (gmailTokensRetrivalCode).

##### Redirect with Codes
- Replace `{jwt}` and `{gmail}` in _redirectUrl with the respective retrieval codes.
- Send the user to that final URL.

#### Importance in Architecture
- This class bridges Google OAuth2 authentication with your JWT-based backend security.
- It decouples sensitive tokens from direct URL transport by using temporary retrieval codes stored in memory.
- It ensures:
  - Users from Google are properly persisted in your DB.
  - Frontend gets short-lived retrieval codes instead of long-lived tokens directly.