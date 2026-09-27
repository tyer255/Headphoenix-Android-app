for f in ["app/src/main/java/com/example/FullScreenLyricsView.kt", "app/src/main/java/com/example/TrackOptionsSheet.kt"]:
    with open(f, "r") as file:
        c = file.read()
    c = c.replace("\\n", "\n")
    with open(f, "w") as file:
        file.write(c)
