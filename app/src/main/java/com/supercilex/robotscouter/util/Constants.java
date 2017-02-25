package com.supercilex.robotscouter.util;

import android.support.annotation.NonNull;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.ChangeEventListener;
import com.firebase.ui.database.FirebaseIndexArray;
import com.firebase.ui.database.SnapshotParser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.supercilex.robotscouter.data.model.Team;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class Constants {
    public static final String MANAGER_STATE = "manager_state";
    public static final String ITEM_COUNT = "count";
    public static final String SCOUT_TEMPLATE = "com.supercilex.robotscouter.scout_template";
    public static final int SINGLE_ITEM = 1;

    /** The list of all supported authentication providers in Firebase Auth UI. */
    public static final List<AuthUI.IdpConfig> ALL_PROVIDERS =
            Collections.unmodifiableList(
                    Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.TWITTER_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.FACEBOOK_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build(),
                                  new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build()));

    // *** CAUTION--DO NOT TOUCH! ***
    // [START FIREBASE CHILD NAMES]
    public static final DatabaseReference FIREBASE_USERS = DatabaseHelper.getRef().child("users");

    // Team
    public static final DatabaseReference FIREBASE_TEAMS_REF =
            DatabaseHelper.getRef().child("teams");
    public static final DatabaseReference FIREBASE_TEAM_INDICES =
            DatabaseHelper.getRef().child("team-indices");
    public static final String FIREBASE_TIMESTAMP = "timestamp";

    public static final SnapshotParser<Team> TEAM_PARSER = new SnapshotParser<Team>() {
        @Override
        public Team parseSnapshot(DataSnapshot snapshot) {
            Team team = snapshot.getValue(Team.class);
            team.setKey(snapshot.getKey());
            return team;
        }
    };

    // Scout
    public static final DatabaseReference FIREBASE_SCOUTS = DatabaseHelper.getRef().child("scouts");
    public static final DatabaseReference FIREBASE_SCOUT_INDICES =
            DatabaseHelper.getRef().child("scout-indices");
    public static final String FIREBASE_METRICS = "metrics";

    // Scout views
    public static final String FIREBASE_VALUE = "value";
    public static final String FIREBASE_TYPE = "type";
    public static final String FIREBASE_NAME = "name";
    public static final String FIREBASE_UNIT = "unit";
    public static final String FIREBASE_SELECTED_VALUE = "selectedValueIndex";

    // Scout template
    public static final DatabaseReference FIREBASE_DEFAULT_TEMPLATE =
            DatabaseHelper.getRef().child("default-template");
    public static final DatabaseReference FIREBASE_SCOUT_TEMPLATES =
            DatabaseHelper.getRef().child("scout-templates");
    public static final String FIREBASE_TEMPLATE_KEY = "templateKey";
    // [END FIREBASE CHILD NAMES]

    public static final String HTML_IMPORT_TEAM;

    public static FirebaseIndexArray sFirebaseTeams;

    static {
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            private final ChangeEventListener mListener = new ChangeEventListener() {
                @Override
                public void onChildChanged(EventType type, int index, int oldIndex) {
                    // Noop
                }

                @Override
                public void onDataChanged() {
                    // Noop
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    // Noop
                }
            };

            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth auth) {
                if (auth.getCurrentUser() == null) {
                    if (sFirebaseTeams != null) {
                        sFirebaseTeams.removeAllListeners();
                        sFirebaseTeams = null;
                    }
                } else {
                    sFirebaseTeams = new FirebaseIndexArray(
                            FIREBASE_TEAM_INDICES.child(auth.getCurrentUser().getUid()),
                            FIREBASE_TEAMS_REF);
                    sFirebaseTeams.addChangeEventListener(mListener);
                }
            }
        });

        HTML_IMPORT_TEAM = "<!doctype html>\n" +
                "<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:o=\"urn:schemas-microsoft-com:office:office\">\n" +
                "\t<head>\n" +
                "\t\t<!-- NAME: 1 COLUMN -->\n" +
                "\t\t<!--[if gte mso 15]>\n" +
                "\t\t<xml>\n" +
                "\t\t\t<o:OfficeDocumentSettings>\n" +
                "\t\t\t<o:AllowPNG/>\n" +
                "\t\t\t<o:PixelsPerInch>96</o:PixelsPerInch>\n" +
                "\t\t\t</o:OfficeDocumentSettings>\n" +
                "\t\t</xml>\n" +
                "\t\t<![endif]-->\n" +
                "\t\t<meta charset=\"UTF-8\">\n" +
                "        <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                "        <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                "\t\t<title>*|MC:SUBJECT|*</title>\n" +
                "        \n" +
                "    <style type=\"text/css\">\n" +
                "\t\tp{\n" +
                "\t\t\tmargin:10px 0;\n" +
                "\t\t\tpadding:0;\n" +
                "\t\t}\n" +
                "\t\ttable{\n" +
                "\t\t\tborder-collapse:collapse;\n" +
                "\t\t}\n" +
                "\t\th1,h2,h3,h4,h5,h6{\n" +
                "\t\t\tdisplay:block;\n" +
                "\t\t\tmargin:0;\n" +
                "\t\t\tpadding:0;\n" +
                "\t\t}\n" +
                "\t\timg,a img{\n" +
                "\t\t\tborder:0;\n" +
                "\t\t\theight:auto;\n" +
                "\t\t\toutline:none;\n" +
                "\t\t\ttext-decoration:none;\n" +
                "\t\t}\n" +
                "\t\tbody,#bodyTable,#bodyCell{\n" +
                "\t\t\theight:100%%;\n" +
                "\t\t\tmargin:0;\n" +
                "\t\t\tpadding:0;\n" +
                "\t\t\twidth:100%%;\n" +
                "\t\t}\n" +
                "\t\t#outlook a{\n" +
                "\t\t\tpadding:0;\n" +
                "\t\t}\n" +
                "\t\timg{\n" +
                "\t\t\t-ms-interpolation-mode:bicubic;\n" +
                "\t\t}\n" +
                "\t\ttable{\n" +
                "\t\t\tmso-table-lspace:0pt;\n" +
                "\t\t\tmso-table-rspace:0pt;\n" +
                "\t\t}\n" +
                "\t\t.ReadMsgBody{\n" +
                "\t\t\twidth:100%%;\n" +
                "\t\t}\n" +
                "\t\t.ExternalClass{\n" +
                "\t\t\twidth:100%%;\n" +
                "\t\t}\n" +
                "\t\tp,a,li,td,blockquote{\n" +
                "\t\t\tmso-line-height-rule:exactly;\n" +
                "\t\t}\n" +
                "\t\ta[href^=tel],a[href^=sms]{\n" +
                "\t\t\tcolor:inherit;\n" +
                "\t\t\tcursor:default;\n" +
                "\t\t\ttext-decoration:none;\n" +
                "\t\t}\n" +
                "\t\tp,a,li,td,body,table,blockquote{\n" +
                "\t\t\t-ms-text-size-adjust:100%%;\n" +
                "\t\t\t-webkit-text-size-adjust:100%%;\n" +
                "\t\t}\n" +
                "\t\t.ExternalClass,.ExternalClass p,.ExternalClass td,.ExternalClass div,.ExternalClass span,.ExternalClass font{\n" +
                "\t\t\tline-height:100%%;\n" +
                "\t\t}\n" +
                "\t\ta[x-apple-data-detectors]{\n" +
                "\t\t\tcolor:inherit !important;\n" +
                "\t\t\ttext-decoration:none !important;\n" +
                "\t\t\tfont-size:inherit !important;\n" +
                "\t\t\tfont-family:inherit !important;\n" +
                "\t\t\tfont-weight:inherit !important;\n" +
                "\t\t\tline-height:inherit !important;\n" +
                "\t\t}\n" +
                "\t\t#bodyCell{\n" +
                "\t\t\tpadding:10px;\n" +
                "\t\t}\n" +
                "\t\t.templateContainer{\n" +
                "\t\t\tmax-width:600px !important;\n" +
                "\t\t}\n" +
                "\t\ta.mcnButton{\n" +
                "\t\t\tdisplay:block;\n" +
                "\t\t}\n" +
                "\t\t.mcnImage{\n" +
                "\t\t\tvertical-align:bottom;\n" +
                "\t\t}\n" +
                "\t\t.mcnTextContent{\n" +
                "\t\t\tword-break:break-word;\n" +
                "\t\t}\n" +
                "\t\t.mcnTextContent img{\n" +
                "\t\t\theight:auto !important;\n" +
                "\t\t}\n" +
                "\t\t.mcnDividerBlock{\n" +
                "\t\t\ttable-layout:fixed !important;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Background Style\n" +
                "\t@tip Set the background color and top border for your email. You may want to choose colors that match your company's branding.\n" +
                "\t*/\n" +
                "\t\tbody,#bodyTable{\n" +
                "\t\t\t/*@editable*/background-color:#FAFAFA;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Background Style\n" +
                "\t@tip Set the background color and top border for your email. You may want to choose colors that match your company's branding.\n" +
                "\t*/\n" +
                "\t\t#bodyCell{\n" +
                "\t\t\t/*@editable*/border-top:0;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Email Border\n" +
                "\t@tip Set the border for your email.\n" +
                "\t*/\n" +
                "\t\t.templateContainer{\n" +
                "\t\t\t/*@editable*/border:0;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Heading 1\n" +
                "\t@tip Set the styling for all first-level headings in your emails. These should be the largest of your headings.\n" +
                "\t@style heading 1\n" +
                "\t*/\n" +
                "\t\th1{\n" +
                "\t\t\t/*@editable*/color:#202020;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:26px;\n" +
                "\t\t\t/*@editable*/font-style:normal;\n" +
                "\t\t\t/*@editable*/font-weight:bold;\n" +
                "\t\t\t/*@editable*/line-height:125%%;\n" +
                "\t\t\t/*@editable*/letter-spacing:normal;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Heading 2\n" +
                "\t@tip Set the styling for all second-level headings in your emails.\n" +
                "\t@style heading 2\n" +
                "\t*/\n" +
                "\t\th2{\n" +
                "\t\t\t/*@editable*/color:#202020;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:22px;\n" +
                "\t\t\t/*@editable*/font-style:normal;\n" +
                "\t\t\t/*@editable*/font-weight:bold;\n" +
                "\t\t\t/*@editable*/line-height:125%%;\n" +
                "\t\t\t/*@editable*/letter-spacing:normal;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Heading 3\n" +
                "\t@tip Set the styling for all third-level headings in your emails.\n" +
                "\t@style heading 3\n" +
                "\t*/\n" +
                "\t\th3{\n" +
                "\t\t\t/*@editable*/color:#202020;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:20px;\n" +
                "\t\t\t/*@editable*/font-style:normal;\n" +
                "\t\t\t/*@editable*/font-weight:bold;\n" +
                "\t\t\t/*@editable*/line-height:125%%;\n" +
                "\t\t\t/*@editable*/letter-spacing:normal;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Page\n" +
                "\t@section Heading 4\n" +
                "\t@tip Set the styling for all fourth-level headings in your emails. These should be the smallest of your headings.\n" +
                "\t@style heading 4\n" +
                "\t*/\n" +
                "\t\th4{\n" +
                "\t\t\t/*@editable*/color:#202020;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:18px;\n" +
                "\t\t\t/*@editable*/font-style:normal;\n" +
                "\t\t\t/*@editable*/font-weight:bold;\n" +
                "\t\t\t/*@editable*/line-height:125%%;\n" +
                "\t\t\t/*@editable*/letter-spacing:normal;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Preheader\n" +
                "\t@section Preheader Style\n" +
                "\t@tip Set the background color and borders for your email's preheader area.\n" +
                "\t*/\n" +
                "\t\t#templatePreheader{\n" +
                "\t\t\t/*@editable*/background-color:#FAFAFA;\n" +
                "\t\t\t/*@editable*/background-image:none;\n" +
                "\t\t\t/*@editable*/background-repeat:no-repeat;\n" +
                "\t\t\t/*@editable*/background-position:center;\n" +
                "\t\t\t/*@editable*/background-size:cover;\n" +
                "\t\t\t/*@editable*/border-top:0;\n" +
                "\t\t\t/*@editable*/border-bottom:0;\n" +
                "\t\t\t/*@editable*/padding-top:9px;\n" +
                "\t\t\t/*@editable*/padding-bottom:9px;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Preheader\n" +
                "\t@section Preheader Text\n" +
                "\t@tip Set the styling for your email's preheader text. Choose a size and color that is easy to read.\n" +
                "\t*/\n" +
                "\t\t#templatePreheader .mcnTextContent,#templatePreheader .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/color:#656565;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:12px;\n" +
                "\t\t\t/*@editable*/line-height:150%%;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Preheader\n" +
                "\t@section Preheader Link\n" +
                "\t@tip Set the styling for your email's preheader links. Choose a color that helps them stand out from your text.\n" +
                "\t*/\n" +
                "\t\t#templatePreheader .mcnTextContent a,#templatePreheader .mcnTextContent p a{\n" +
                "\t\t\t/*@editable*/color:#656565;\n" +
                "\t\t\t/*@editable*/font-weight:normal;\n" +
                "\t\t\t/*@editable*/text-decoration:underline;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Header\n" +
                "\t@section Header Style\n" +
                "\t@tip Set the background color and borders for your email's header area.\n" +
                "\t*/\n" +
                "\t\t#templateHeader{\n" +
                "\t\t\t/*@editable*/background-color:#FFFFFF;\n" +
                "\t\t\t/*@editable*/background-image:none;\n" +
                "\t\t\t/*@editable*/background-repeat:no-repeat;\n" +
                "\t\t\t/*@editable*/background-position:center;\n" +
                "\t\t\t/*@editable*/background-size:cover;\n" +
                "\t\t\t/*@editable*/border-top:0;\n" +
                "\t\t\t/*@editable*/border-bottom:0;\n" +
                "\t\t\t/*@editable*/padding-top:9px;\n" +
                "\t\t\t/*@editable*/padding-bottom:0;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Header\n" +
                "\t@section Header Text\n" +
                "\t@tip Set the styling for your email's header text. Choose a size and color that is easy to read.\n" +
                "\t*/\n" +
                "\t\t#templateHeader .mcnTextContent,#templateHeader .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/color:#202020;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:16px;\n" +
                "\t\t\t/*@editable*/line-height:150%%;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Header\n" +
                "\t@section Header Link\n" +
                "\t@tip Set the styling for your email's header links. Choose a color that helps them stand out from your text.\n" +
                "\t*/\n" +
                "\t\t#templateHeader .mcnTextContent a,#templateHeader .mcnTextContent p a{\n" +
                "\t\t\t/*@editable*/color:#2BAADF;\n" +
                "\t\t\t/*@editable*/font-weight:normal;\n" +
                "\t\t\t/*@editable*/text-decoration:underline;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Body\n" +
                "\t@section Body Style\n" +
                "\t@tip Set the background color and borders for your email's body area.\n" +
                "\t*/\n" +
                "\t\t#templateBody{\n" +
                "\t\t\t/*@editable*/background-color:#FFFFFF;\n" +
                "\t\t\t/*@editable*/background-image:none;\n" +
                "\t\t\t/*@editable*/background-repeat:no-repeat;\n" +
                "\t\t\t/*@editable*/background-position:center;\n" +
                "\t\t\t/*@editable*/background-size:cover;\n" +
                "\t\t\t/*@editable*/border-top:0;\n" +
                "\t\t\t/*@editable*/border-bottom:2px solid #EAEAEA;\n" +
                "\t\t\t/*@editable*/padding-top:0;\n" +
                "\t\t\t/*@editable*/padding-bottom:9px;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Body\n" +
                "\t@section Body Text\n" +
                "\t@tip Set the styling for your email's body text. Choose a size and color that is easy to read.\n" +
                "\t*/\n" +
                "\t\t#templateBody .mcnTextContent,#templateBody .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/color:#202020;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:16px;\n" +
                "\t\t\t/*@editable*/line-height:150%%;\n" +
                "\t\t\t/*@editable*/text-align:left;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Body\n" +
                "\t@section Body Link\n" +
                "\t@tip Set the styling for your email's body links. Choose a color that helps them stand out from your text.\n" +
                "\t*/\n" +
                "\t\t#templateBody .mcnTextContent a,#templateBody .mcnTextContent p a{\n" +
                "\t\t\t/*@editable*/color:#2BAADF;\n" +
                "\t\t\t/*@editable*/font-weight:normal;\n" +
                "\t\t\t/*@editable*/text-decoration:underline;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Footer\n" +
                "\t@section Footer Style\n" +
                "\t@tip Set the background color and borders for your email's footer area.\n" +
                "\t*/\n" +
                "\t\t#templateFooter{\n" +
                "\t\t\t/*@editable*/background-color:#FAFAFA;\n" +
                "\t\t\t/*@editable*/background-image:none;\n" +
                "\t\t\t/*@editable*/background-repeat:no-repeat;\n" +
                "\t\t\t/*@editable*/background-position:center;\n" +
                "\t\t\t/*@editable*/background-size:cover;\n" +
                "\t\t\t/*@editable*/border-top:0;\n" +
                "\t\t\t/*@editable*/border-bottom:0;\n" +
                "\t\t\t/*@editable*/padding-top:9px;\n" +
                "\t\t\t/*@editable*/padding-bottom:9px;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Footer\n" +
                "\t@section Footer Text\n" +
                "\t@tip Set the styling for your email's footer text. Choose a size and color that is easy to read.\n" +
                "\t*/\n" +
                "\t\t#templateFooter .mcnTextContent,#templateFooter .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/color:#656565;\n" +
                "\t\t\t/*@editable*/font-family:Helvetica;\n" +
                "\t\t\t/*@editable*/font-size:12px;\n" +
                "\t\t\t/*@editable*/line-height:150%%;\n" +
                "\t\t\t/*@editable*/text-align:center;\n" +
                "\t\t}\n" +
                "\t/*\n" +
                "\t@tab Footer\n" +
                "\t@section Footer Link\n" +
                "\t@tip Set the styling for your email's footer links. Choose a color that helps them stand out from your text.\n" +
                "\t*/\n" +
                "\t\t#templateFooter .mcnTextContent a,#templateFooter .mcnTextContent p a{\n" +
                "\t\t\t/*@editable*/color:#656565;\n" +
                "\t\t\t/*@editable*/font-weight:normal;\n" +
                "\t\t\t/*@editable*/text-decoration:underline;\n" +
                "\t\t}\n" +
                "\t@media only screen and (min-width:768px){\n" +
                "\t\t.templateContainer{\n" +
                "\t\t\twidth:600px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\tbody,table,td,p,a,li,blockquote{\n" +
                "\t\t\t-webkit-text-size-adjust:none !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\tbody{\n" +
                "\t\t\twidth:100%% !important;\n" +
                "\t\t\tmin-width:100%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t#bodyCell{\n" +
                "\t\t\tpadding-top:10px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImage{\n" +
                "\t\t\twidth:100%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnCartContainer,.mcnCaptionTopContent,.mcnRecContentContainer,.mcnCaptionBottomContent,.mcnTextContentContainer,.mcnBoxedTextContentContainer,.mcnImageGroupContentContainer,.mcnCaptionLeftTextContentContainer,.mcnCaptionRightTextContentContainer,.mcnCaptionLeftImageContentContainer,.mcnCaptionRightImageContentContainer,.mcnImageCardLeftTextContentContainer,.mcnImageCardRightTextContentContainer{\n" +
                "\t\t\tmax-width:100%% !important;\n" +
                "\t\t\twidth:100%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnBoxedTextContentContainer{\n" +
                "\t\t\tmin-width:100%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImageGroupContent{\n" +
                "\t\t\tpadding:9px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnCaptionLeftContentOuter .mcnTextContent,.mcnCaptionRightContentOuter .mcnTextContent{\n" +
                "\t\t\tpadding-top:9px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImageCardTopImageContent,.mcnCaptionBlockInner .mcnCaptionTopContent:last-child .mcnTextContent{\n" +
                "\t\t\tpadding-top:18px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImageCardBottomImageContent{\n" +
                "\t\t\tpadding-bottom:9px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImageGroupBlockInner{\n" +
                "\t\t\tpadding-top:0 !important;\n" +
                "\t\t\tpadding-bottom:0 !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImageGroupBlockOuter{\n" +
                "\t\t\tpadding-top:9px !important;\n" +
                "\t\t\tpadding-bottom:9px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnTextContent,.mcnBoxedTextContentColumn{\n" +
                "\t\t\tpadding-right:18px !important;\n" +
                "\t\t\tpadding-left:18px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcnImageCardLeftImageContent,.mcnImageCardRightImageContent{\n" +
                "\t\t\tpadding-right:18px !important;\n" +
                "\t\t\tpadding-bottom:0 !important;\n" +
                "\t\t\tpadding-left:18px !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t\t.mcpreview-image-uploader{\n" +
                "\t\t\tdisplay:none !important;\n" +
                "\t\t\twidth:100%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Heading 1\n" +
                "\t@tip Make the first-level headings larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\th1{\n" +
                "\t\t\t/*@editable*/font-size:22px !important;\n" +
                "\t\t\t/*@editable*/line-height:125%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Heading 2\n" +
                "\t@tip Make the second-level headings larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\th2{\n" +
                "\t\t\t/*@editable*/font-size:20px !important;\n" +
                "\t\t\t/*@editable*/line-height:125%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Heading 3\n" +
                "\t@tip Make the third-level headings larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\th3{\n" +
                "\t\t\t/*@editable*/font-size:18px !important;\n" +
                "\t\t\t/*@editable*/line-height:125%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Heading 4\n" +
                "\t@tip Make the fourth-level headings larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\th4{\n" +
                "\t\t\t/*@editable*/font-size:16px !important;\n" +
                "\t\t\t/*@editable*/line-height:150%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Boxed Text\n" +
                "\t@tip Make the boxed text larger in size for better readability on small screens. We recommend a font size of at least 16px.\n" +
                "\t*/\n" +
                "\t\t.mcnBoxedTextContentContainer .mcnTextContent,.mcnBoxedTextContentContainer .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/font-size:14px !important;\n" +
                "\t\t\t/*@editable*/line-height:150%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Preheader Visibility\n" +
                "\t@tip Set the visibility of the email's preheader on small screens. You can hide it to save space.\n" +
                "\t*/\n" +
                "\t\t#templatePreheader{\n" +
                "\t\t\t/*@editable*/display:block !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Preheader Text\n" +
                "\t@tip Make the preheader text larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\t#templatePreheader .mcnTextContent,#templatePreheader .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/font-size:14px !important;\n" +
                "\t\t\t/*@editable*/line-height:150%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Header Text\n" +
                "\t@tip Make the header text larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\t#templateHeader .mcnTextContent,#templateHeader .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/font-size:16px !important;\n" +
                "\t\t\t/*@editable*/line-height:150%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Body Text\n" +
                "\t@tip Make the body text larger in size for better readability on small screens. We recommend a font size of at least 16px.\n" +
                "\t*/\n" +
                "\t\t#templateBody .mcnTextContent,#templateBody .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/font-size:16px !important;\n" +
                "\t\t\t/*@editable*/line-height:150%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}\t@media only screen and (max-width: 480px){\n" +
                "\t/*\n" +
                "\t@tab Mobile Styles\n" +
                "\t@section Footer Text\n" +
                "\t@tip Make the footer content text larger in size for better readability on small screens.\n" +
                "\t*/\n" +
                "\t\t#templateFooter .mcnTextContent,#templateFooter .mcnTextContent p{\n" +
                "\t\t\t/*@editable*/font-size:14px !important;\n" +
                "\t\t\t/*@editable*/line-height:150%% !important;\n" +
                "\t\t}\n" +
                "\n" +
                "}</style></head>\n" +
                "    <body>\n" +
                "        <center>\n" +
                "            <table align=\"center\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" height=\"100%%\" width=\"100%%\" id=\"bodyTable\">\n" +
                "                <tr>\n" +
                "                    <td align=\"center\" valign=\"top\" id=\"bodyCell\">\n" +
                "                        <!-- BEGIN TEMPLATE // -->\n" +
                "\t\t\t\t\t\t<!--[if gte mso 9]>\n" +
                "\t\t\t\t\t\t<table align=\"center\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"600\" style=\"width:600px;\">\n" +
                "\t\t\t\t\t\t<tr>\n" +
                "\t\t\t\t\t\t<td align=\"center\" valign=\"top\" width=\"600\" style=\"width:600px;\">\n" +
                "\t\t\t\t\t\t<![endif]-->\n" +
                "                        <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"templateContainer\">\n" +
                "                            <tr>\n" +
                "                                <td valign=\"top\" id=\"templatePreheader\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"mcnTextBlock\" style=\"min-width:100%%;\">\n" +
                "    <tbody class=\"mcnTextBlockOuter\">\n" +
                "        <tr>\n" +
                "            <td valign=\"top\" class=\"mcnTextBlockInner\" style=\"padding-top:9px;\">\n" +
                "              \t<!--[if mso]>\n" +
                "\t\t\t\t<table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%%\" style=\"width:100%%;\">\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "\t\t\t    \n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t<td valign=\"top\" width=\"599\" style=\"width:599px;\">\n" +
                "\t\t\t\t<![endif]-->\n" +
                "                <table align=\"left\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:100%%; min-width:100%%;\" width=\"100%%\" class=\"mcnTextContentContainer\">\n" +
                "                    <tbody><tr>\n" +
                "                        \n" +
                "                        <td valign=\"top\" class=\"mcnTextContent\" style=\"padding-top:0; padding-right:18px; padding-left:18px;\">\n" +
                "                        \n" +
                "                            <h1 class=\"null\"><span style=\"font-family:roboto,helvetica neue,helvetica,arial,sans-serif\">Robot Scouter</span></h1>\n" +
                "\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </tbody></table>\n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t</td>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "                \n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t\t</table>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table></td>\n" +
                "                            </tr>\n" +
                "                            <tr>\n" +
                "                                <td valign=\"top\" id=\"templateHeader\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"mcnTextBlock\" style=\"min-width:100%%;\">\n" +
                "    <tbody class=\"mcnTextBlockOuter\">\n" +
                "        <tr>\n" +
                "            <td valign=\"top\" class=\"mcnTextBlockInner\">\n" +
                "              \t<!--[if mso]>\n" +
                "\t\t\t\t<table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%%\" style=\"width:100%%;\">\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "\t\t\t    \n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t<td valign=\"top\" width=\"599\" style=\"width:599px;\">\n" +
                "\t\t\t\t<![endif]-->\n" +
                "                <table align=\"left\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:100%%; min-width:100%%;\" width=\"100%%\" class=\"mcnTextContentContainer\">\n" +
                "                    <tbody><tr>\n" +
                "                        \n" +
                "                        <td valign=\"top\" class=\"mcnTextContent\" style=\"padding: 0px 18px 9px; font-family: Roboto, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif;\">\n" +
                "                        \n" +
                "                            <span style=\"font-size:14px\"><span style=\"font-family:roboto,helvetica neue,helvetica,arial,sans-serif\">You were invited to edit team %s in Robot Scouter:</span></span>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </tbody></table>\n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t</td>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "                \n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t\t</table>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"mcnButtonBlock\" style=\"min-width:100%%;\">\n" +
                "    <tbody class=\"mcnButtonBlockOuter\">\n" +
                "        <tr>\n" +
                "            <td style=\"padding-top:0; padding-right:18px; padding-bottom:9px; padding-left:18px;\" valign=\"top\" align=\"left\" class=\"mcnButtonBlockInner\">\n" +
                "                <table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"mcnButtonContentContainer\" style=\"border-collapse: separate !important;border-radius: 3px;background-color: #2947DD;\">\n" +
                "                    <tbody>\n" +
                "                        <tr>\n" +
                "                            <td align=\"center\" valign=\"middle\" class=\"mcnButtonContent\" style=\"font-family: Roboto, &quot;Helvetica Neue&quot;, Helvetica, Arial, sans-serif; font-size: 16px; padding: 15px;\">\n" +
                "                                <a class=\"mcnButton \" title=\"Import team %s\" href=\"%%%%APPINVITE_LINK_PLACEHOLDER%%%%\" target=\"_blank\" style=\"font-weight: bold;letter-spacing: normal;line-height: 100%%;text-align: center;text-decoration: none;color: #FFFFFF;\">Import team 2521 - SERT</a>\n" +
                "                            </td>\n" +
                "                        </tr>\n" +
                "                    </tbody>\n" +
                "                </table>\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"mcnImageBlock\" style=\"min-width:100%%;\">\n" +
                "    <tbody class=\"mcnImageBlockOuter\">\n" +
                "            <tr>\n" +
                "                <td valign=\"top\" style=\"padding:9px\" class=\"mcnImageBlockInner\">\n" +
                "                    <table align=\"left\" width=\"100%%\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" class=\"mcnImageContentContainer\" style=\"min-width:100%%;\">\n" +
                "                        <tbody><tr>\n" +
                "                            <td class=\"mcnImageContent\" valign=\"top\" style=\"padding-right: 9px; padding-left: 9px; padding-top: 0; padding-bottom: 0;\">\n" +
                "                                \n" +
                "                                    \n" +
                "                                        <img align=\"left\" alt=\"\" src=\"%s\" width=\"300\" style=\"max-width:1000px; padding-bottom: 0; display: inline !important; vertical-align: bottom;\" class=\"mcnImage\">\n" +
                "                                    \n" +
                "                                \n" +
                "                            </td>\n" +
                "                        </tr>\n" +
                "                    </tbody></table>\n" +
                "                </td>\n" +
                "            </tr>\n" +
                "    </tbody>\n" +
                "</table><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"mcnTextBlock\" style=\"min-width:100%%;\">\n" +
                "    <tbody class=\"mcnTextBlockOuter\">\n" +
                "        <tr>\n" +
                "            <td valign=\"top\" class=\"mcnTextBlockInner\" style=\"padding-top:9px;\">\n" +
                "              \t<!--[if mso]>\n" +
                "\t\t\t\t<table align=\"left\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" width=\"100%%\" style=\"width:100%%;\">\n" +
                "\t\t\t\t<tr>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "\t\t\t    \n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t<td valign=\"top\" width=\"599\" style=\"width:599px;\">\n" +
                "\t\t\t\t<![endif]-->\n" +
                "                <table align=\"left\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:100%%; min-width:100%%;\" width=\"100%%\" class=\"mcnTextContentContainer\">\n" +
                "                    <tbody><tr>\n" +
                "                        \n" +
                "                        <td valign=\"top\" class=\"mcnTextContent\" style=\"padding-top:0; padding-right:18px; padding-bottom:9px; padding-left:18px;\">\n" +
                "                        \n" +
                "                            <span style=\"font-size:12px\"><span style=\"font-family:roboto,helvetica neue,helvetica,arial,sans-serif\">Thank you for using Robot Scouter!</span></span>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </tbody></table>\n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t</td>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "                \n" +
                "\t\t\t\t<!--[if mso]>\n" +
                "\t\t\t\t</tr>\n" +
                "\t\t\t\t</table>\n" +
                "\t\t\t\t<![endif]-->\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table></td>\n" +
                "                            </tr>\n" +
                "                            <tr>\n" +
                "                                <td valign=\"top\" id=\"templateBody\"></td>\n" +
                "                            </tr>\n" +
                "                            <tr>\n" +
                "                                <td valign=\"top\" id=\"templateFooter\"><table border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" class=\"mcnDividerBlock\" style=\"min-width:100%%;\">\n" +
                "    <tbody class=\"mcnDividerBlockOuter\">\n" +
                "        <tr>\n" +
                "            <td class=\"mcnDividerBlockInner\" style=\"min-width: 100%%; padding: 10px 18px 25px;\">\n" +
                "                <table class=\"mcnDividerContent\" border=\"0\" cellpadding=\"0\" cellspacing=\"0\" width=\"100%%\" style=\"min-width: 100%%;border-top: 2px solid #EEEEEE;\">\n" +
                "                    <tbody><tr>\n" +
                "                        <td>\n" +
                "                            <span></span>\n" +
                "                        </td>\n" +
                "                    </tr>\n" +
                "                </tbody></table>\n" +
                "<!--            \n" +
                "                <td class=\"mcnDividerBlockInner\" style=\"padding: 18px;\">\n" +
                "                <hr class=\"mcnDividerContent\" style=\"border-bottom-color:none; border-left-color:none; border-right-color:none; border-bottom-width:0; border-left-width:0; border-right-width:0; margin-top:0; margin-right:0; margin-bottom:0; margin-left:0;\" />\n" +
                "-->\n" +
                "            </td>\n" +
                "        </tr>\n" +
                "    </tbody>\n" +
                "</table></td>\n" +
                "                            </tr>\n" +
                "                        </table>\n" +
                "\t\t\t\t\t\t<!--[if gte mso 9]>\n" +
                "\t\t\t\t\t\t</td>\n" +
                "\t\t\t\t\t\t</tr>\n" +
                "\t\t\t\t\t\t</table>\n" +
                "\t\t\t\t\t\t<![endif]-->\n" +
                "                        <!-- // END TEMPLATE -->\n" +
                "                    </td>\n" +
                "                </tr>\n" +
                "            </table>\n" +
                "        </center>\n" +
                "    </body>\n" +
                "</html>";
    }

    private Constants() {
        // no instance
    }

    public static void init() {
        // Needed for java to perform class initialization
    }

    public static DatabaseReference getScoutMetrics(String key) {
        return FIREBASE_SCOUTS.child(key).child(FIREBASE_METRICS);
    }
}
