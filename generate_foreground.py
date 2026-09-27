xml = """<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    
    <!-- Top Wave -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M 30,42 C 45,34 65,34 80,42 C 82,43 83,46 82,48 C 81,50 78,51 76,50 C 63,43 47,43 34,50 C 32,51 29,50 28,48 C 27,46 28,43 30,42 Z" />
        
    <!-- Middle Wave -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M 32,56 C 45,50 63,50 76,56 C 78,57 79,60 78,61 C 77,63 74,64 72,63 C 61,58 47,58 36,63 C 34,64 31,63 30,61 C 29,60 30,57 32,56 Z" />
        
    <!-- Bottom Wave -->
    <path
        android:fillColor="#FFFFFF"
        android:pathData="M 36,69 C 46,65 60,65 70,69 C 72,70 73,72 72,74 C 71,76 69,76 67,75 C 59,72 47,72 39,75 C 37,76 35,76 34,74 C 33,72 34,70 36,69 Z" />

    <!-- Crown Body -->
    <path
        android:fillColor="#FFD700"
        android:pathData="M 68,18 L 72,30 L 78,20 L 86,28 L 94,18 L 94,36 L 68,36 Z" />
        
    <!-- Crown Base -->
    <path
        android:fillColor="#FFA500"
        android:pathData="M 68,38 L 94,38 L 94,42 L 68,42 Z" />
        
    <!-- Jewel 1 (Left) -->
    <path
        android:fillColor="#FF0000"
        android:pathData="M 67,16 C 67,15 69,15 69,16 C 69,17 67,17 67,16 Z" />
        
    <!-- Jewel 2 (Middle) -->
    <path
        android:fillColor="#00FF00"
        android:pathData="M 77,18 C 77,17 79,17 79,18 C 79,19 77,19 77,18 Z" />
        
    <!-- Jewel 3 (Right) -->
    <path
        android:fillColor="#0000FF"
        android:pathData="M 93,16 C 93,15 95,15 95,16 C 95,17 93,17 93,16 Z" />
</vector>"""
with open("app/src/main/res/drawable/ic_launcher_foreground.xml", "w") as f:
    f.write(xml)
