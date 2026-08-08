package com.quizmaker.android.repository;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class LeaderboardRepository_Factory implements Factory<LeaderboardRepository> {
  private final Provider<SupabaseClient> supabaseProvider;

  public LeaderboardRepository_Factory(Provider<SupabaseClient> supabaseProvider) {
    this.supabaseProvider = supabaseProvider;
  }

  @Override
  public LeaderboardRepository get() {
    return newInstance(supabaseProvider.get());
  }

  public static LeaderboardRepository_Factory create(Provider<SupabaseClient> supabaseProvider) {
    return new LeaderboardRepository_Factory(supabaseProvider);
  }

  public static LeaderboardRepository newInstance(SupabaseClient supabase) {
    return new LeaderboardRepository(supabase);
  }
}
