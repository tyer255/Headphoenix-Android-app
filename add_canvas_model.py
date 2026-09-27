import re

with open('app/src/main/java/com/example/data/remote/models/ApiModels.kt', 'r') as f:
    content = f.read()

model = """
@Serializable
data class CanvasDto(
    val requestedTrackId: String? = null,
    val canvasUrl: String? = null,
    val videoUrl: String? = null
)
"""

if "data class CanvasDto" not in content:
    content += "\n" + model
    with open('app/src/main/java/com/example/data/remote/models/ApiModels.kt', 'w') as f:
        f.write(content)
