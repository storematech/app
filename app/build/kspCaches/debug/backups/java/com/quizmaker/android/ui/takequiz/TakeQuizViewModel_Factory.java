package com.quizmaker.android.ui.takequiz;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.QuizTakingRepository;
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
public final class TakeQuizViewModel_Factory implements Factory<TakeQuizViewModel> {
  private final Provider<QuizTakingRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public TakeQuizViewModel_Factory(Provider<QuizTakingRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public TakeQuizViewModel get() {
    return newInstance(repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static TakeQuizViewModel_Factory create(Provider<QuizTakingRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new TakeQuizViewModel_Factory(repositoryProvider, savedStateHandleProvider);
  }

  public static TakeQuizViewModel newInstance(QuizTakingRepository repository,
      SavedStateHandle savedStateHandle) {
    return new TakeQuizViewModel(repository, savedStateHandle);
  }
}
