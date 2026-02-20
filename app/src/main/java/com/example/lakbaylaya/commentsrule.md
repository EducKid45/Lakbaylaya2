KDoc Quick Template Table
Type Template Example
File kotlin /** Handles <file responsibility>. */ kotlin /** Handles navigation logic for the map
screen. */
Class kotlin /** Manages <class responsibility>. */ kotlin /** Manages navigation state and steps.
*/ class NavigationViewModel : ViewModel()
Function kotlin /** <Function description> @param paramName Description @return Description */
kotlin /** Checks if the user reached the next navigation step. @param location Current user
location @return true if the step is reached */ fun checkStepArrival(location: Location): Boolean
Simple Function kotlin // <Short description> fun functionName()    kotlin // Update navigation
progress fun updateProgress()
Property / Variable kotlin /** <Property description> */ kotlin /** Current index of the navigation
step. */ var currentStepIndex: Int = 0
Inline (tricky line)    kotlin // <Short intent description>    kotlin // Stop navigation when user
is close enough stopNavigation()
✅ Rules for Using This Table

Keep all comments short, direct, and descriptive.

Only use KDoc for files, classes, functions, and properties when needed.

Inline comments are for tricky logic only.

No labels like WHAT/ROLE/PURPOSE — just direct sentences.

No WHY explanations — Copilot can infer it.

Use @param and @return for clarity when a function has inputs/outputs.