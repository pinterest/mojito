package com.box.l10n.mojito;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import org.springdoc.core.converters.models.SortObject;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A custom {@link ModelResolver} class to hide fields that are not annotated with the {@link
 * JsonView} annotation in Swagger
 */
public class CustomModelResolver extends ModelResolver {
  public CustomModelResolver(ObjectMapper mapper) {
    super(mapper);
  }

  @Override
  public Schema<?> resolve(
      AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> next) {
    if (annotatedType.getType().getTypeName().equals(SortObject.class.getTypeName())) {
      ObjectSchema objectSchema = new ObjectSchema();
      objectSchema.setName("SortObject");
      objectSchema.setProperties(
          Map.of(
              "empty",
              new BooleanSchema(),
              "sorted",
              new BooleanSchema(),
              "unsorted",
              new BooleanSchema()));
      return objectSchema;
    }
    if (annotatedType.getJsonViewAnnotation() != null
        && annotatedType.getCtxAnnotations() != null) {
      boolean hasRequestBodyAnnotation =
          Arrays.stream(annotatedType.getCtxAnnotations())
              .anyMatch(
                  annotation ->
                      annotation
                          .annotationType()
                          .getTypeName()
                          .equals(RequestBody.class.getTypeName()));
      if (hasRequestBodyAnnotation) {
        annotatedType.jsonViewAnnotation(null);
        return super.resolve(annotatedType, context, next);
      }
    }
    return super.resolve(annotatedType, context, next);
  }

  @Override
  protected boolean hiddenByJsonView(Annotation[] annotations, AnnotatedType type) {
    JsonView jsonView = type.getJsonViewAnnotation();
    if (jsonView == null) return false;
    Class<?>[] filters = jsonView.value();
    for (Annotation ant : annotations) {
      if (ant instanceof JsonView) {
        Class<?>[] views = ((JsonView) ant).value();
        for (Class<?> f : filters) {
          for (Class<?> v : views) {
            if (v == f || v.isAssignableFrom(f)) {
              return false;
            }
          }
        }
      }
    }
    return !type.getType().getTypeName().startsWith(Page.class.getTypeName());
  }
}
