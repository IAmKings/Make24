package com.twentyfoursolve.app.di

import com.twentyfoursolve.data.DataModule
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module(
    includes = [DataModule::class]
)
@InstallIn(SingletonComponent::class)
object AppModule