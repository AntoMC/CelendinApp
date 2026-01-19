# 1. REGLA MAESTRA PARA EL ERROR DE CASTING
-keepattributes Signature, *Annotation*, InnerClasses, EnclosingMethod, MethodParameters

# 2. Impedir que R8 toque tus modelos y la interfaz de red
-keep class com.amc.celendinapp.model.** { *; }
-keep interface com.amc.celendinapp.network.** { *; }

# 3. Reglas específicas para que Retrofit y GSON vean los tipos genéricos
-keep class retrofit2.** { *; }
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }

# 4. Forzar que NO se borren los tipos de las listas en las respuestas de la API
-keepclassmembers class com.amc.celendinapp.model.RespuestaAdinelsa {
    <fields>;
}

# 5. Mantener los nombres de los parámetros (n_idgen_grupo, etc.)
-keepclassmembers interface com.amc.celendinapp.network.AdinelsaApiService {
    <methods>;
}

# 6. Evitar que se borre el soporte de reflexión
-keep class java.lang.reflect.ParameterizedType { *; }