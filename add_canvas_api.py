with open('app/src/main/java/com/example/data/remote/ApiService.kt', 'r') as f:
    content = f.read()

api_method = """    @GET("canvas")
    suspend fun getCanvas(@Query("id") id: String): Response<ApiResponse<CanvasDto>>"""

if "fun getCanvas" not in content:
    content = content.replace('suspend fun resolvePlayback', api_method + '\n\n    @POST("playback/resolve")\n    suspend fun resolvePlayback')
    with open('app/src/main/java/com/example/data/remote/ApiService.kt', 'w') as f:
        f.write(content)
