package com.quizmaker.android.ui.quizdetailview;

import androidx.lifecycle.SavedStateHandle;
import com.quizmaker.android.repository.QuizDetailViewRepository;
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
public final class QuizDetailViewViewModel_Factory implements Factory<QuizDetailViewViewModel> {
  private final Provider<QuizDetailViewRepository> repositoryProvider;

  private final Provider<SavedStateHandle> savedStateHandleProvider;

  public QuizDetailViewViewModel_Factory(Provider<QuizDetailViewRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    this.repositoryProvider = repositoryProvider;
    this.savedStateHandleProvider = savedStateHandleProvider;
  }

  @Override
  public QuizDetailViewViewModel get() {
    return newInstance(repositoryProvider.get(), savedStateHandleProvider.get());
  }

  public static QuizDetailViewViewModel_Factory create(
      Provider<QuizDetailViewRepository> repositoryProvider,
      Provider<SavedStateHandle> savedStateHandleProvider) {
    return new QuizDetailViewViewModel_Factory(repositoryProvider, savedStateHandleProvider);
  }

  public static QuizDetailViewViewModel newInstance(QuizDetailViewRepository repository,
      SavedStateHandle savedStateHandle) {
    return new QuizDetailViewViewModel(repository, savedStateHandle);
  }
}
