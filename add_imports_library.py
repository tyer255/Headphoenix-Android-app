with open("app/src/main/java/com/example/LibraryScreen.kt", "r") as f:
    content = f.read()

imports_to_add = """
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.collectIsPressedAsState
"""

# add after import androidx.compose.ui.unit.sp
if "import androidx.compose.ui.unit.sp" in content:
    content = content.replace("import androidx.compose.ui.unit.sp", "import androidx.compose.ui.unit.sp" + imports_to_add)

with open("app/src/main/java/com/example/LibraryScreen.kt", "w") as f:
    f.write(content)
