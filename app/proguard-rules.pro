# Keeps line numbers and file name obfuscation
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

# Kotlin
-dontwarn kotlin.**
-dontwarn com.supercilex.robotscouter.util.data.TeamsLiveData$merger$1$mergeTeams$2$1$1

# TODO temporary until AC reaches 1.0
-keep class * implements android.arch.lifecycle.GeneratedAdapter {<init>(...);}

# In-app billing
-keep class com.android.vending.billing.**

# Retrofit
-dontnote retrofit2.Platform
-dontwarn retrofit2.**
-dontwarn okio.**
-dontwarn okhttp3.**

# Other
-dontnote com.google.**
-dontnote com.facebook.**

# Remove logging
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Apache POI
-dontwarn org.apache.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn org.etsi.**
-dontwarn org.w3.**
-dontwarn com.microsoft.schemas.**
-dontwarn com.graphbuilder.**
-dontnote org.apache.**
-dontnote org.openxmlformats.schemas.**
-dontnote org.etsi.**
-dontnote org.w3.**
-dontnote com.microsoft.schemas.**
-dontnote com.graphbuilder.**

-keeppackagenames org.apache.poi.ss.formula.function

-keep class com.fasterxml.aalto.stax.InputFactoryImpl
-keep class com.fasterxml.aalto.stax.OutputFactoryImpl
-keep class com.fasterxml.aalto.stax.EventFactoryImpl

-keep class schemaorg_apache_xmlbeans.system.sF1327CCA741569E70F9CA8C9AF9B44B2.TypeSystemHolder {
    public final static *** typeSystem;
}

-keep class org.apache.xmlbeans.impl.schema.BuiltinSchemaTypeSystem {
    public static *** get(...); public static *** getNoType(...);
}
-keep class org.apache.xmlbeans.impl.schema.PathResourceLoader { public <init>(...); }
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeSystemCompiler { public static *** compile(...); }
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeSystemImpl {
    public <init>(...); public static *** get(...); public static *** getNoType(...);
}
-keep class org.apache.xmlbeans.impl.schema.SchemaTypeLoaderImpl {
    public static *** getContextTypeLoader(...); public static *** build(...);
}
-keep class org.apache.xmlbeans.impl.store.Locale {
    public static *** streamToNode(...); public static *** nodeTo*(...);
}
-keep class org.apache.xmlbeans.impl.store.Path { public static *** compilePath(...); }
-keep class org.apache.xmlbeans.impl.store.Query { public static *** compileQuery(...); }

-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBooleanProperty { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookView { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBookViews { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorder { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorders { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTBorderPr { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCell { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellAlignment { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellFormula { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellStyleXfs { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCellXfs { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTColor { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCol { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTCols { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTDrawing { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFill { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFills { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFont { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFonts { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontName { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontScheme { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTFontSize { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTIntProperty { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmt { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTNumFmts { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCell { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTMergeCells { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPatternFill { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPageMargins { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTPane { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRow { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSelection { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheet { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetData { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetDimension { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetFormatPr { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetView { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheetViews { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSheets { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTSst { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTStylesheet { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTRst { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbook { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorkbookPr { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTWorksheet { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.CTXf { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.SstDocument { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.StyleSheetDocument { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellType$Enum { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.STCellFormulaType$Enum { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.STXstring { *; }

-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBooleanPropertyImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBookViewsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBordersImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTBorderPrImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellAlignmentImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellFormulaImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellStyleXfsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTCellXfsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColorImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTColsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTDrawingImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFillsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontNameImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSchemeImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTFontSizeImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTIntPropertyImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTNumFmtImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTNumFmtsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTMergeCellImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTMergeCellsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPatternFillImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPageMarginsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTPaneImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRowImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSelectionImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetDataImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetDimensionImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetFormatPrImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetViewImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetViewsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSheetsImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTSstImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTStylesheetImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTRstImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorkbookPrImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTWorksheetImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.CTXfImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.SstDocumentImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.StyleSheetDocumentImpl { *; }
-keep class org.openxmlformats.schemas.spreadsheetml.x2006.main.impl.STXstringImpl { *; }

-keep class org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.CTPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.customProperties.impl.PropertiesDocumentImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.CTPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.officeDocument.x2006.extendedProperties.impl.PropertiesDocumentImpl { *; }

-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTAxDataSource { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTAxPos { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTBoolean { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTCatAx { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTChart { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTChartSpace { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTCrossBetween { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTCrosses { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTHeaderFooter { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTLayout { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTLegendPos { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTLegend { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTLineChart { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTLineSer { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTMarker { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTMarkerStyle { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTNumData { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTNumDataSource { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTNumRef { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTNumVal { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTOrientation { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTPageMargins { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTPageSetup { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTPlotArea { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTPrintSettings { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTScaling { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterChart { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterSer { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTScatterStyle { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTSerTx { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTStrData { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTStrRef { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTStrVal { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTTickLblPos { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTTickMark { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTTitle { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTTx { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTUnsignedInt { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.CTValAx { *; }

-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTAxDataSourceImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTAxPosImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTBooleanImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTCatAxImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTChartImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTChartSpaceImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTCrossBetweenImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTCrossesImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTHeaderFooterImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTLayoutImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTLegendImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTLegendPosImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTLineChartImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTLineSerImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTMarkerImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTMarkerStyleImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTNumDataImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTNumDataSourceImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTNumRefImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTNumValImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTOrientationImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTPageMarginsImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTPageSetupImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTPlotAreaImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTPrintSettingsImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTScalingImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTScatterChartImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTScatterSerImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTScatterStyleImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTSerTxImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTStrDataImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTStrRefImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTStrValImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTTickLblPosImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTTickMarkImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTTitleImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTTxImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTUnsignedIntImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.chart.impl.CTValAxImpl { *; }

-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObject { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTGraphicalObjectData { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualDrawingProps { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTNonVisualGraphicFrameProperties { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTPoint2D { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTPositiveSize2D { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTextBodyProperties { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTextCharacterProperties { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTextListStyle { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraphProperties { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.CTTransform2D { *; }

-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTGraphicalObjectImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTGraphicalObjectDataImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTNonVisualDrawingPropsImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTNonVisualGraphicFramePropertiesImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTPoint2DImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTPositiveSize2DImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTRegularTextRunImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextBodyImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextBodyPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextCharacterPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextListStyleImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextParagraphImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTextParagraphPropertiesImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.main.impl.CTTransform2DImpl { *; }

-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTAnchorClientData { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTDrawing { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrame { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGraphicalObjectFrameNonVisual { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTTwoCellAnchor { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.STEditAs { *; }

-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTAnchorClientDataImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTDrawingImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTGraphicalObjectFrameImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTGraphicalObjectFrameNonVisualImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTMarkerImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.CTTwoCellAnchorImpl { *; }
-keep class org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.impl.STEditAsImpl { *; }
