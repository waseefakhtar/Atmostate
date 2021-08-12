package ca.ramzan.atmostate.di

import android.app.Application
import ca.ramzan.atmostate.database.AtmostateDatabase
import ca.ramzan.atmostate.database.cities.CityDatabaseDao
import ca.ramzan.atmostate.database.weather.WeatherDatabaseDao
import ca.ramzan.atmostate.network.WeatherApi
import ca.ramzan.atmostate.repository.WeatherRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    @Provides
    @Singleton
    fun atmostateDatabase(app: Application) = AtmostateDatabase.getInstance(app)


    @Provides
    @Singleton
    fun cityDao(db: AtmostateDatabase) = db.cityDatabaseDao

    @Provides
    @Singleton
    fun weatherDao(db: AtmostateDatabase) = db.weatherDatabaseDao

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun weatherApi(moshi: Moshi) = WeatherApi(moshi)

    @Provides
    @Singleton
    fun weatherRepo(weatherDao: WeatherDatabaseDao, cityDao: CityDatabaseDao, api: WeatherApi) =
        WeatherRepository(weatherDao, cityDao, api)

}
