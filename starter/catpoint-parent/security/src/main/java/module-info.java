module com.udacity.catpoint.security {
    requires com.udacity.catpoint.image;
    requires com.google.common;
    requires com.google.gson;
    requires miglayout;
    requires java.desktop;
    requires java.prefs;

    opens com.udacity.catpoint.security.data to com.google.gson;
}