import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer

fun test(context: android.content.Context) {
    val renderersFactory = DefaultRenderersFactory(context).setEnableDecoderFallback(true)
    val builder = ExoPlayer.Builder(context)
    builder.setRenderersFactory(renderersFactory)
}
