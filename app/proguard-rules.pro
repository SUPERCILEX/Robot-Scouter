# Optimize
-mergeinterfacesaggressively
-repackageclasses com.supercilex.robotscouter

# Retrofit
-dontwarn okio.**
-dontnote retrofit2.Platform
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
-dontwarn retrofit2.Platform$Java8
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions

# Apache POI
-dontwarn org.apache.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.graphbuilder.**

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

-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.** { *; } # TODO

#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookView { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookViews { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontName { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontScheme { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontSize { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIntProperty { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetData { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetDimension { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetFormatPr { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetView { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheets { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSst { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTStylesheet { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookPr { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.SstDocument { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.StyleSheetDocument { *; }
#
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewsImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderPrImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColorImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontNameImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSchemeImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSizeImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTIntPropertyImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPatternFillImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetDataImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetDimensionImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetFormatPrImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetViewImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetViewsImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetsImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSstImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTStylesheetImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookPrImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorksheetImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTXfImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.SstDocumentImpl { *; }
#-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.StyleSheetDocumentImpl { *; }

-keep class org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.CTPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.PropertiesDocumentImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.CTPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.PropertiesDocumentImpl { *; }
