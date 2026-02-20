SECTION: Language and Platform

- Always use Kotlin.
- Target Android SDK.
- Prefer modern Android APIs and Jetpack libraries.
- Use Jetpack Compose for UI unless explicitly instructed otherwise.

SECTION: Architecture

- Use MVVM (Model–View–ViewModel) architecture.
- All business logic goes in ViewModels. Do not create a domain layer.
- Separate code into:
    - data/ (API, repository, models)
    - ui/ (Activities, Fragments, ViewModels, screens, components)
- Do NOT place networking, database, or business logic inside screens.
- Use Repository pattern for all data sources.
- Screens orchestrate components and bind to ViewModel only.
- Folder rules:
    - Split code into multiple focused files when logic grows.
    - Automatically create subfolders to group related files if a folder becomes large.
- Example base folder structure:
  data/
  api/
  repository/
  model/
  ui/
  screen/
  viewmodel/
  component/
  theme/
  util/
  constants/

SECTION: Mapping and API Usage

- Use MapLibre for map rendering.
- Use Geoapify for routing, geocoding, and navigation APIs.
- Do NOT use Google Maps SDK or other providers unless explicitly instructed.
- Default navigation mode is walking.
- Always handle API/network errors gracefully (e.g., invalid routes, GPS unavailability, network
  failures).

SECTION: Accessibility (Blind Navigation Priority)

- All UI must be fully compatible with TalkBack.
- Provide contentDescription for every interactive element.
- Do NOT rely solely on visual indicators (color, icons).
- Prefer voice feedback (TextToSpeech) and haptic feedback.
- Avoid small touch targets and gestures requiring vision.
- Ensure linear reading order for audio navigation, even outside TalkBack screens.

SECTION: TalkBack Screen Structure

- Screens customizing TalkBack focus or reading order must be in their own subfolder.
- Control which elements receive focus and define reading order explicitly.
- Arrange UI elements logically for linear audio navigation.
- Do NOT rely on visual layout order alone.
- Reusable components must be in ui/component/ and never mix component logic inside screens.
- If a screen or component folder becomes large, automatically create subfolders to maintain
  organization.

SECTION: UI Theme, Color, and Typography

- Define separate files for light and dark modes (e.g., ThemeLight.kt, ThemeDark.kt).
- Create reusable containers for colors, typography, and styles.
- Semantic naming required:
    - Colors: PrimaryBackground, SecondaryText, AccentColor
    - Typography: Header, Body, Caption
- Do NOT hardcode colors, fonts, or sizes in UI.
- Specify a default theme in Android Manifest.
- For Compose or runtime theme switching, always reference theme containers in code.
- Ensure accessibility compliance (contrast, font size, readability) in both light and dark modes.
- AI must decide optimal structure for color containers, typography hierarchy, and theme
  composition.

SECTION: Coding Style and Naming Conventions

- Classes: PascalCase (e.g., MyViewModel, NavigationScreen)
- Functions/variables: camelCase (e.g., fetchRoutes, currentStep)
- Use meaningful names.
- Avoid magic numbers; use constants.
- Prefer val over var.
- Keep functions small, focused, and readable.
- Add comments only for complex or non-obvious logic.
- Do NOT generate unnecessary boilerplate.

SECTION: Gradle and Build Rules

- Use Kotlin DSL only (build.gradle.kts).
- Do NOT use Groovy DSL.
- Do NOT mix DSL styles in the same project.
- Use version catalogs if available.
- Do NOT add unnecessary dependencies.

SECTION: Coding Principles and Style

- Apply SOLID principles where applicable.
- Apply DRY (Do Not Repeat Yourself).
- Apply KISS (Keep It Simple).
- Maintain clear separation of concerns.
- Apply comments and documentation principles for readability.
- Imperative style for operational logic, sequential processing, TTS commands, and navigation
  updates.
- Declarative style for UI state management (e.g., Compose).
- Favor clarity and readability over cleverness.
- For thesis prototype, balance principles with simplicity and functionality.

SECTION: File Size and Structure

- Do NOT put large amounts of logic in a single file.
- Split code into multiple focused files when logic grows.
- Create subfolders automatically for grouping related files (screens, components, large features).
- Organize files by responsibility, not feature size.
- Avoid “god files” with unrelated logic.

SECTION: Code Correctness, Validation, and Testing

- Avoid compilation errors, runtime errors, and warnings whenever possible.
- Do NOT suppress warnings or errors silently.
- If a warning or error occurs:
    - Attempt to solve it immediately before moving to another file.
    - Validate correctness after each fix.
- Make iterative changes and validate correctness before moving to another file.
- Ensure the current file builds logically before editing additional files.
- If stuck in infinite loops or unsolvable situations:
    - Stop
    - Add a clear comment explaining why that part cannot be solved
- Optional: include minimal unit tests or validation logic for critical components (navigation, TTS,
  routing).

SECTION: General Behavior

- Prefer clarity over cleverness.
- Optimize for readability, maintainability, and accessibility.
- Do NOT assume perfect network or GPS.
- Always generate production-quality code.
