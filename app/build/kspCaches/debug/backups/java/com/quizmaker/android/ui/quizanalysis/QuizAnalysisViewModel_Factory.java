package com.quizmaker.android.ui.quizanalysis;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.QuizAnalysisRepository;
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
public final class QuizAnalysisViewModel_Factory implements Factory<QuizAnalysisViewModel> {
  private final Provider<QuizAnalysisRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public QuizAnalysisViewModel_Factory(Provider<QuizAnalysisRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public QuizAnalysisViewModel get() {
    return newInstance(repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static QuizAnalysisViewModel_Factory create(
      Provider<QuizAnalysisRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new QuizAnalysisViewModel_Factory(repositoryProvider, savedStateHandleProvider);
  }

  public static QuizAnalysisViewModel newInstance(QuizAnalysisRepository repository,
      SavedStateHandle savedStateHandle) {
    return new QuizAnalysisViewModel(repository, savedStateHandle);
  }
}
