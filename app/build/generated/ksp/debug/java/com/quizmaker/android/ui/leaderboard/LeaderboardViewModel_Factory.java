package com.quizmaker.android.ui.leaderboard;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.LeaderboardRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class LeaderboardViewModel_Factory implements Factory<LeaderboardViewModel> {
  private final Provider<LeaderboardRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public LeaderboardViewModel_Factory(Provider<LeaderboardRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public LeaderboardViewModel get() {
    return newInstance(repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static LeaderboardViewModel_Factory create(
      Provider<LeaderboardRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new LeaderboardViewModel_Factory(repositoryProvider, savedStateHandleProvider);
  }

  public static LeaderboardViewModel newInstance(LeaderboardRepository repository,
      SavedStateHandle savedStateHandle) {
    return new LeaderboardViewModel(repository, savedStateHandle);
  }
}
