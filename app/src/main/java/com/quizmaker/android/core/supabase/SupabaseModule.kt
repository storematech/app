package com.quizmaker.android.core.supabase

import com.quizmaker.android.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

/**
 * Talks to the exact same Supabase project the Quiz Maker website uses
 * (see src/integrations/supabase/client.ts in the web repo) so data,
 * accounts, and RLS policies are shared between web and Android.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        // Default is 10s, which a two-step "all my quiz ids, then all their responses" query
        // can exceed once an account has more than a handful of quizzes — was surfacing as a
        // hard dashboard failure ("Request timeout has expired ... request_timeout=10000ms").
        requestTimeout = 30.seconds
        install(Auth)
        install(Postgrest)
        install(Realtime)
        install(Storage)
        install(Functions)
    }
}
