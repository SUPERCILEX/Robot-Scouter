# Optimize
-optimizations !field/*,!class/merging/*,*
-repackageclasses com.supercilex.robotscouter
-mergeinterfacesaggressively

# will keep line numbers and file name obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# In-app billing
-keep class com.android.vending.billing.**

# Apache POI
-dontwarn org.apache.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.graphbuilder.**

-keeppackagenames org.apache.poi.ss.formula.function

-keep class com.fasterxml.aalto.stax.InputFactoryImpl
-keep class com.fasterxml.aalto.stax.OutputFactoryImpl
-keep class com.fasterxml.aalto.stax.EventFactoryImpl

-keep class schemaorg_apache_xmlbeans.system.sF1327CCA741569E70F9CA8C9AF9B44B2.TypeSystemHolder { public final static *** typeSystem; }

-keep class org.apache.xmlbeans.impl.schema.BuiltinSchemaTypeSystem { public static *** get(...); public static *** getNoType(...); }
-keep class org.apache.xmlbeans.impl.schema.PathResourceLoader { public <init>(...); }
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeSystemCompiler { public static *** compile(...); }
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl { public <init>(...); public static *** get(...); public static *** getNoType(...); }
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeLoaderImpl { public static *** getContextTypeLoader(...); public static *** build(...); }
-keep class org.apache.xmlbeans.impl.store.Locale { public static *** streamToNode(...); public static *** nodeTo*(...); }
-keep class org.apache.xmlbeans.impl.store.Path { public static *** compilePath(...); }
-keep class org.apache.xmlbeans.impl.store.Query { public static *** compileQuery(...); }

-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.** { *; }

-keep class org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.CTPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.PropertiesDocumentImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.CTPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.PropertiesDocumentImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTDrawingImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTMarkerImpl { *; }
-keep class com.microsoft.schemas.office.office.impl.CTIdMapImpl { *; }
-keep class com.microsoft.schemas.office.office.impl.CTShapeLayoutImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTShadowImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTFillImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTPathImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTShapeImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTShapetypeImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTStrokeImpl { *; }
-keep class com.microsoft.schemas.vml.impl.CTTextboxImpl { *; }
-keep class com.microsoft.schemas.office.excel.impl.CTClientDataImpl { *; }
-keep class com.microsoft.schemas.office.excel.impl.STTrueFalseBlankImpl { *; }
