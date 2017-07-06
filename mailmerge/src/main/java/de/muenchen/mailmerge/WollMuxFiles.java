/*
 * Dateiname: WollMuxFiles.java
 * Projekt  : WollMux
 * Funktion : Managed die Dateien auf die der WollMux zugreift (z.B. wollmux.conf)
 *
 * Copyright (c) 2009-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 13.04.2006 | BNK | Erstellung
 * 20.04.2006 | BNK | [R1200] .wollmux-Verzeichnis als Vorbelegung für DEFAULT_CONTEXT
 * 26.05.2006 | BNK | +DJ Initialisierung
 * 20.06.2006 | BNK | keine wollmux.conf mehr anlegen wenn nicht vorhanden
 *                  | /etc/wollmux/wollmux.conf auswerten
 * 26.06.2006 | BNK | Dialoge/FONT_ZOOM auswerten. LookAndFeel setzen.
 * 07.09.2006 | BNK | isDebugMode effizienter gemacht.
 * 21.09.2006 | BNK | Unter Windows nach c:\programme\wollmux\wollmux.conf schauen
 * 19.10.2006 | BNK | +dumpInfo()
 * 05.12.2006 | BNK | +getClassPath()
 * 20.12.2006 | BNK | CLASSPATH:Falls keine Dateierweiterung angegeben, / ans Ende setzen, weil nur so Verzeichnisse erkannt werden.
 * 09.07.2007 | BNK | [R7134]Popup, wenn Server langsam
 * 09.07.2007 | BNK | [R7137]IP-Adresse in Dumpinfo
 * 17.07.2007 | BNK | [R7605]Dateien binär kopieren in dumpInfo(), außerdem immer als UTF-8 schreiben
 * 18.07.2007 | BNK | Alle Java-Properties in dumpInfo() ausgeben
 * 27.07.2007 | BNK | [P1448]WollMuxClassLoader.class.getClassLoader() als parent verwenden
 * 01.09.2008 | BNK | [R28149]Klassen im CLASSPATH aus wollmux.conf haben vorrang vor WollMux-internen.
 * 18.08.2009 | BED | -defaultWollmuxConf
 *                  | andere Strategie für Suche nach wollmux.conf in setupWollMuxDir()
 *                  | Verzeichnis der wollmux.conf als Default für DEFAULT_CONTEXT
 * 12.01.2010 | BED | dumpInfo() gibt nun auch JVM Heap Size + verwendeten Speicher aus
 * 07.10.2010 | ERT | dumpInfo() erweitert um No. of Processors, Physical Memory und Swap Size
 * 19.10.2010 | ERT | dumpInfo() erweitert um IP-Adresse und OOo-Version
 * 22.02.2011 | ERT | dumpInfo() erweitert um LHM-Version
 * 08.05.2012 | jub | fakeSymLink behandlung eingebaut: der verweis auf fragmente, wie er in der
 *                    config datei steht, kann auf einen sog. fake SymLink gehen, eine text-
 *                    datei, in der auf ein anderes fragment inkl. relativem pfad verwiesen wird.
 * 11.12.2012 | jub | fakeSymLinks werden doch nicht gebraucht; wieder aus dem code entfernt
 * 17.05.2013 | ukt | Fontgröße wird jetzt immer gesetzt, unabhängig davon, ob der Wert in der
 *                    wollmuxbar.conf gesetzt ist oder nicht.
 *                    Andernfalls wird die Änderung der Fontgröße von einem Nicht-Defaultwert auf
 *                    den Default-Wert nicht angezeigt, wenn alle anderen Optionswerte ebenfalls
 *                    den Default-Wert haben.
 *
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.mailmerge;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.ShlObj;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinReg;
import com.sun.star.lib.loader.RegistryAccess;
import com.sun.star.lib.loader.RegistryAccessException;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;
import de.muenchen.mailmerge.dialog.Common;

/**
 *
 * Managed die Dateien auf die der WollMux zugreift (z,B, wollmux,conf)
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class WollMuxFiles
{
  private static final String ETC_WOLLMUX_WOLLMUX_CONF = "/etc/wollmux/wollmux.conf";

  /**
   * Der Pfad (ohne Wurzel wie HKCU oder HKLM) zu dem Registrierungsschlüssel, unter
   * dem der WollMux seine Registry-Werte speichert
   */
  private static final String WOLLMUX_KEY = "Software\\WollMux";

  /**
   * Der Name des String-Wertes, unter dem der WollMux in der Registry den Ort der
   * wollmux.conf speichert
   */
  private static final String WOLLMUX_CONF_PATH_VALUE_NAME = "ConfigPath";

  private static final String WOLLMUX_NOCONF =
    L.m("Es wurde keine WollMux-Konfiguration (wollmux.conf) gefunden - deshalb läuft WollMux im NoConfig-Modus.");

  /**
   * Wenn nach dieser Anzahl Millisekunden die Konfiguration noch nicht vollständig
   * eingelesen ist, wird ein Popup mit der Meldung {@link #SLOW_SERVER_MESSAGE}
   * gebracht.
   */
  private static final long SLOW_SERVER_TIMEOUT = 10000;

  /**
   * Siehe {@link #SLOW_SERVER_TIMEOUT}.
   */
  private static final String SLOW_SERVER_MESSAGE =
    L.m("Ihr Vorlagen-Server und/oder Ihre Netzwerkverbindung sind sehr langsam.\nDies kann die Arbeit mit OpenOffice.org stark beeinträchtigen.");

  /**
   * Die in der wollmux.conf mit DEFAULT_CONTEXT festgelegte URL.
   */
  private static URL defaultContextURL;

  /**
   * Enthält den geparsten Konfigruationsbaum der wollmux.conf
   */
  private static ConfigThingy wollmuxConf;

  /**
   * Das Verzeichnis ,wollmux.
   */
  private static File wollmuxDir;

  /**
   * Enthält einen PrintStream in den die Log-Nachrichten geschrieben werden.
   */
  private static File wollmuxLogFile;

  /**
   * Enthält das File der Konfigurationsdatei wollmux.conf
   */
  private static File wollmuxConfFile;

  /**
   * Enthält das File in des local-overwrite-storage-caches.
   */
  private static File losCacheFile;

  /**
   * Gibt an, ob der debug-Modus aktiviert ist.
   */
  private static boolean debugMode;

  /**
   * Erzeugt das .wollmux-Verzeichnis im Home-Verzeichnis des Benutzers (falls es
   * noch nicht existiert), sucht nach der wollmux.conf und parst sie. Initialisiert
   * auch den Logger.
   * <p>
   * Die wollmux.conf wird an folgenden Stellen in der angegebenen Reihenfolge
   * gesucht:
   *
   * <ol>
   * <li>unter dem Dateipfad (inkl. Dateiname!), der im Registrierungswert
   * "ConfigPath" des Schlüssels HKCU\Software\WollMux\ festgelegt ist (nur Windows!)
   * </li>
   * <li>$HOME/.wollmux/wollmux.conf (wobei $HOME unter Windows das Profilverzeichnis
   * bezeichnet)</li>
   * <li>unter dem Dateipfad (inkl. Dateiname!), der im Registrierungswert
   * "ConfigPath" des Schlüssels HKLM\Software\WollMux\ festgelegt ist (nur Windows!)
   * </li>
   * <li>unter dem Dateipfad, der in der Konstanten
   * {@link #C_PROGRAMME_WOLLMUX_WOLLMUX_CONF} festgelegt ist (nur Windows!)</li>
   * <li>unter dem Dateipfad, der in der Konstanten {@link #ETC_WOLLMUX_WOLLMUX_CONF}
   * festgelegt ist (nur Linux!)</li>
   * </ol>
   *
   * @return false für den den Fall no Config, true bei gefundener wollmux.conf
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  public static boolean setupWollMuxDir()
  {
    long time = System.currentTimeMillis(); // Zeitnahme fürs Debuggen
    boolean noConf = false; // kein no conf mode

    String userHome = System.getProperty("user.home");
    wollmuxDir = new File(userHome, ".wollmux");

    // .wollmux-Verzeichnis im userHome erzeugen falls es nicht existiert
    // Selbst wenn die wollmux.conf nicht im .wollmux-Verzeichnis liegt,
    // wird es dennoch für die cache.conf und wollmux.log benötigt
    if (!wollmuxDir.exists())
      wollmuxDir.mkdirs();

    // cache.conf und wollmux.log im .wollmux-Verzeichnis
    losCacheFile = new File(wollmuxDir, "cache.conf");
    wollmuxLogFile = new File(wollmuxDir, "mailmerge.log");

    StringBuilder debug2Messages = new StringBuilder();
    try
    {
      findWollMuxConf(debug2Messages);
      debug2Messages.append("wollmux.conf gefunden in "
          + wollmuxConfFile.getAbsolutePath());
        debug2Messages.append('\n');
    }
    catch (FileNotFoundException ex)
    {
      debug2Messages.append(ex.getLocalizedMessage());
      debug2Messages.append('\n');
    }

    // Bevor wir versuchen zu parsen wird auf jeden Fall ein leeres ConfigThingy
    // angelegt, damit wollmuxConf auch dann wohldefiniert ist, wenn die Datei
    // Fehler enthält bzw. fehlt.
    wollmuxConf = new ConfigThingy("wollmuxConf");

    SlowServerWatchdog fido = new SlowServerWatchdog(SLOW_SERVER_TIMEOUT);

    // Jetzt versuchen, die wollmux.conf zu parsen, falls sie existiert
    if (wollmuxConfFile != null && wollmuxConfFile.exists() && wollmuxConfFile.isFile())
    {
      fido.start();
      try
      {
        wollmuxConf =
          new ConfigThingy("wollmuxConf", wollmuxConfFile.toURI().toURL());
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }
    else
    { // wollmux.conf existiert nicht - damit wechseln wir in den no config mode.
      noConf = true;
      Logger.log(WOLLMUX_NOCONF);
    }

    fido.dontBark();
    fido.logTimes();

    // Logging-Mode setzen
    setLoggingMode(WollMuxFiles.getWollmuxConf());

    // Gesammelte debug2 Messages rausschreiben
    Logger.debug2(debug2Messages.toString());

    // Lokalisierung initialisieren
    ConfigThingy l10n = getWollmuxConf().query("L10n", 1);
    if (l10n.count() > 0)
      L.init(l10n);
    Logger.debug(L.flushDebugMessages());

    determineDefaultContext();

    initDebugMode();

    setLookAndFeel();

    Logger.debug(L.m(".wollmux init time: %1ms", ""
      + (System.currentTimeMillis() - time)));

    return !noConf;
  }

  private static void findWollMuxConf(StringBuilder debug2Messages)
      throws FileNotFoundException
  {
    // Überprüfen, ob das Betriebssystem Windows ist
    boolean windowsOS =
      System.getProperty("os.name").toLowerCase().contains("windows");

    // Zum Aufsammeln der Pfade, an denen die wollmux.conf gesucht wurde:
    ArrayList<String> searchPaths = new ArrayList<String>();

    // Logger initialisieren:
    Logger.init(wollmuxLogFile, Logger.LOG);

    // Pfad zur wollmux.conf
    String wollmuxConfPath;

    // wollmux.conf wird über die Umgebungsvariable "WOLLMUX_CONF_PATH" gesetzt.
    if (wollmuxConfFile == null || !wollmuxConfFile.exists())
    {
      if (System.getenv("WOLLMUX_CONF_PATH") != null)
      {
        wollmuxConfPath = System.getenv("WOLLMUX_CONF_PATH");
        searchPaths.add(wollmuxConfPath);
      }

      searchPaths.add(new File(wollmuxDir, "wollmux.conf").getAbsolutePath());
      searchPaths.add(System.getProperty("user.dir") + "/.wollmux/wollmux.conf");
      searchPaths.add(ETC_WOLLMUX_WOLLMUX_CONF);

      // Falls Windows:
      // Versuch den Pfad der wollmux.conf aus HKCU-Registry zu lesen
      if (windowsOS)
      {
        try
        {
          wollmuxConfPath =
            RegistryAccess.getStringValueFromRegistry(WinReg.HKEY_CURRENT_USER,
              WOLLMUX_KEY, WOLLMUX_CONF_PATH_VALUE_NAME);
          searchPaths.add(wollmuxConfPath);

          wollmuxConfPath =
            RegistryAccess.getStringValueFromRegistry(WinReg.HKEY_LOCAL_MACHINE,
              WOLLMUX_KEY, WOLLMUX_CONF_PATH_VALUE_NAME);

          searchPaths.add(wollmuxConfPath);
        }
        catch (RegistryAccessException ex)
        {}

        Shell32 shell = Shell32.INSTANCE;

        char[] arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_APPDATA, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");

        arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILESX86, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");

        arrWollmuxConfPath = new char[WinDef.MAX_PATH];
        shell.SHGetFolderPath(null, ShlObj.CSIDL_PROGRAM_FILES, null,
          ShlObj.SHGFP_TYPE_CURRENT, arrWollmuxConfPath);
        searchPaths.add(Native.toString(arrWollmuxConfPath)
          + "/.wollmux/wollmux.conf");
      }

      for (String path : searchPaths)
      {
        File confPath = new File(path);
        if (confPath.exists())
        {
          wollmuxConfFile = confPath;
          return;
        }
      }

      throw new FileNotFoundException(
        "wollmux.conf konnte nicht gefunden werden. Suchpfade.");
    }
  }

  /**
   * Liefert das File-Objekt der wollmux,conf zurück, die gelesen wurde (kann z,B,
   * auch die aus /etc/wollmux/ sein). Darf erst nach setupWollMuxDir() aufgerufen
   * werden.
   */
  public static File getWollMuxConfFile()
  {
    return wollmuxConfFile;
  }

  /**
   * Liefert das File-Objekt des LocalOverrideStorage Caches zurück. Darf erst nach
   * setupWollMuxDir() aufgerufen werden.
   *
   * @return das File-Objekt des LocalOverrideStorage Caches.
   */
  public static File getLosCacheFile()
  {
    return losCacheFile;
  }

  /**
   * Liefert den Inhalt der wollmux,conf zurück.
   */
  public static ConfigThingy getWollmuxConf()
  {
    return wollmuxConf;
  }

  /**
   * Diese Methode liefert den letzten in der Konfigurationsdatei definierten
   * DEFAULT_CONTEXT zurück. Ist in der Konfigurationsdatei keine URL definiert bzw.
   * ist die Angabe fehlerhaft, so wird die URL des .wollmux Verzeichnisses
   * zurückgeliefert.
   */
  public static URL getDEFAULT_CONTEXT()
  {
    return defaultContextURL;
  }

  /**
   * Liefert eine URL zum String urlStr, wobei relative Pfade relativ zum
   * DEFAULT_CONTEXT aufgelöst werden.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   * @throws MalformedURLException
   *           falls urlStr keine legale URL darstellt.
   */
  public static URL makeURL(String urlStr) throws MalformedURLException
  {
    return new URL(WollMuxFiles.getDEFAULT_CONTEXT(), ConfigThingy.urlEncode(urlStr));
  }

  /**
   * Wertet den DEFAULT_CONTEXT aus wollmux.conf aus und erstellt eine entsprechende
   * URL, mit der {@link #defaultContextURL} initialisiert wird. Wenn in der
   * wollmux.conf kein DEFAULT_CONTEXT angegeben ist, so wird das Verzeichnis, in dem
   * die wollmux.conf gefunden wurde, als Default Context verwendet.
   *
   * Sollte {{@link #defaultContextURL} nicht <code>null</code> sein, tut diese
   * Methode nichts.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1) TESTED
   */
  private static void determineDefaultContext()
  {
    if (defaultContextURL == null)
    {
      ConfigThingy dc = getWollmuxConf().query("DEFAULT_CONTEXT");
      String urlStr;
      try
      {
        urlStr = dc.getLastChild().toString();
      }
      catch (NodeNotFoundException e)
      {
        urlStr = "./";
      }

      // url mit einem "/" aufhören lassen (falls noch nicht angegeben).
      String urlVerzStr;
      if (urlStr.endsWith("/") || urlStr.endsWith("\\"))
        urlVerzStr = urlStr;
      else
        urlVerzStr = urlStr + "/";

      try
      {
        /*
         * Die folgenden 3 Statements realisieren ein Fallback-Verhalten. Falls das
         * letzte Statement eine MalformedURLException wirft, dann gilt das vorige
         * Statement. Hat dieses schon eine MalformedURLException geworfen (sollte
         * eigentlich nicht passieren können), so gilt immer noch das erste.
         */
        defaultContextURL = new URL("file:///");
        if (getWollMuxConfFile() != null)
        {
          defaultContextURL = getWollMuxConfFile().toURI().toURL();
        }
        defaultContextURL = new URL(defaultContextURL, urlVerzStr);
      }
      catch (MalformedURLException e)
      {
        Logger.error(e);
      }
    }
  }

  /**
   * Wertet die FONT_ZOOM-Direktive des Dialoge-Abschnitts aus und zoomt die Fonts
   * falls erforderlich.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeel()
  {
    Common.setLookAndFeelOnce();
    double zoomFactor = 1.0;
    ConfigThingy zoom = getWollmuxConf().query("Dialoge").query("FONT_ZOOM", 2);
    if (zoom.count() > 0)
    {
      try
      {
        zoomFactor = Double.parseDouble(zoom.getLastChild().toString());
      }
      catch (Exception x)
      {
        Logger.error(x);
      }
    }
    zoomFonts(zoomFactor);
  }

  /**
   * Zoomt die Fonts auf zoomFactor, falls erforderlich.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void zoomFonts(double zoomFactor)
  {

    if (zoomFactor < 0.5 || zoomFactor > 10)
    {
      Logger.error(L.m("Unsinniger FONT_ZOOM Wert angegeben: %1", "" + zoomFactor));
    }
    else
    {
      // Frühere Prüfung (nur werte kleiner 0.99 oder größer 1.01) entfernt,
      // seitdem die Fontgröße im Gui eingestellt werden kann.
      // Mit dieser Prüfung wäre z. b. ein Zurückstellen
      // von Zoomfaktor 2 auf 1 abgelehnt worden.
      //Common.zoomFonts(zoomFactor);
    }

  }

  /**
   * Wertet die wollmux,conf-Direktive LOGGING_MODE aus und setzt den Logging-Modus
   * entsprechend. Ist kein LOGGING_MODE gegeben, so greift der Standard (siehe
   * Logger.java)
   *
   * @param ct
   */
  private static void setLoggingMode(ConfigThingy ct)
  {
    ConfigThingy log = ct.query("LOGGING_MODE");
    if (log.count() > 0)
    {
      try
      {
        String mode = log.getLastChild().toString();
        Logger.init(mode);
      }
      catch (NodeNotFoundException x)
      {
        Logger.error(x);
      }
    }
  }

  /**
   * Gibt Auskunft darüber, sich der WollMux im debug-modus befindet; Der debug-modus
   * wird automatisch aktiviert, wenn der LOGGING_MODE auf "debug" oder "all" gesetzt
   * wurde. Im debug-mode werden z.B. die Bookmarks abgearbeiteter Dokumentkommandos
   * nach der Ausführung nicht entfernt, damit sich Fehler leichter finden lassen.
   *
   * @return
   */
  public static boolean isDebugMode()
  {
    return debugMode;
  }

  private static void initDebugMode()
  {
    ConfigThingy log = getWollmuxConf().query("LOGGING_MODE");
    if (log.count() > 0)
    {
      try
      {
        String mode = log.getLastChild().toString();
        if (mode.compareToIgnoreCase("debug") == 0
          || mode.compareToIgnoreCase("all") == 0)
        {
          debugMode = true;
        }
      }
      catch (Exception e)
      {}
    }
    else
      debugMode = false;
  }

  private static class SlowServerWatchdog extends Thread
  {
    private long initTime;

    private long startTime;

    private long endTime;

    private long timeout;

    private long testTime;

    private long dontBarkTime = 0;

    private boolean[] bark = new boolean[] { true };

    public SlowServerWatchdog(long timeout)
    {
      initTime = System.currentTimeMillis();
      this.timeout = timeout;
      setDaemon(true);
    }

    @Override
    public void run()
    {
      startTime = System.currentTimeMillis();
      endTime = startTime + timeout;
      while (true)
      {
        long wait = endTime - System.currentTimeMillis();
        if (wait <= 0)
          break;
        try
        {
          Thread.sleep(wait);
        }
        catch (InterruptedException e)
        {}
      }

      synchronized (bark)
      {
        testTime = System.currentTimeMillis();
        if (!bark[0])
          return;
      }

      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          Logger.error(SLOW_SERVER_MESSAGE);
          JOptionPane pane =
            new JOptionPane(SLOW_SERVER_MESSAGE, JOptionPane.WARNING_MESSAGE,
              JOptionPane.DEFAULT_OPTION);
          JDialog dialog = pane.createDialog(null, L.m("Hinweis"));
          dialog.setModal(false);
          dialog.setVisible(true);
        }
      });
    }

    public void dontBark()
    {
      synchronized (bark)
      {
        dontBarkTime = System.currentTimeMillis();
        bark[0] = false;
      }
    }

    public void logTimes()
    {
      Logger.debug("init: " + initTime + " start: " + startTime + " end: " + endTime
        + " test: " + testTime + " dontBark: " + dontBarkTime);
    }

  }

}
