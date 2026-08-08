package com.quizmaker.android.ui.quizdetail;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.QuizRepository;
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
public final class QuizDetailViewModel_Factory implements Factory<QuizDetailViewModel> {
  private final Provider<QuizRepository> quizRepositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public QuizDetailViewModel_Factory(Provider<QuizRepository> quizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.quizRepositoryProvider = quizRepositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public QuizDetailViewModel get() {
    return newInstance(quizRepositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static QuizDetailViewModel_Factory create(Provider<QuizRepository> quizRepositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new QuizDetailViewModel_Factory(quizRepositoryProvider, savedStateHandleProvider);
  }

  public static QuizDetailViewModel newInstance(QuizRepository quizRepository,
      SavedStateHandle savedStateHandle) {
    return new QuizDetailViewModel(quizRepository, savedStateHandle);
  }
}
