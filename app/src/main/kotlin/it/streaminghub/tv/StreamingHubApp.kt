package it.streaminghub.tv

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.util.DebugLogger
import it.streaminghub.tv.data.local.AppPreferences
import it.streaminghub.tv.data.repository.CatalogRepository
import it.streaminghub.tv.data.repository.FavoritesRepository
import it.streaminghub.tv.data.repository.PlaybackRepository
import it.streaminghub.tv.data.repository.ProfileRepository
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class StreamingHubApp : Application(), ImageLoaderFactory {

    lateinit var preferences: AppPreferences
        private set

    lateinit var catalogRepository: CatalogRepository
        private set

    lateinit var playbackRepository: PlaybackRepository
        private set

    lateinit var profileRepository: ProfileRepository
        private set

    lateinit var favoritesRepository: FavoritesRepository
        private set

    private val coilOkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; Android TV) StreamingHubTV/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        preferences = AppPreferences(this)
        catalogRepository = CatalogRepository(preferences)
        playbackRepository = PlaybackRepository(preferences)
        profileRepository = ProfileRepository(preferences)
        favoritesRepository = FavoritesRepository(preferences)

        // Initialize Coil singleton explicitly
        Coil.setImageLoader(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(coilOkHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024) // 150MB disk cache
                    .build()
            }
            .crossfade(true)
            .logger(DebugLogger())
            .build()
    }
}
