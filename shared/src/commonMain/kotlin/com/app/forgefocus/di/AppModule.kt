package com.app.forgefocus.di

import com.app.forgefocus.core.di.coreModule
import com.app.forgefocus.core.di.databaseModule
import com.app.forgefocus.features.mountains.di.mountainsModule
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(
            platformModule,
            databaseModule,
            mountainsModule
        )
    }
}

fun initKoin() = initKoin {}