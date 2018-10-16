/*
 * Dateiname: Common.java
 * Projekt  : WollMux
 * Funktion : Enthält von den Dialogen gemeinsam genutzten Code.
 *
 * Copyright (c) 2010-2015 Landeshauptstadt München
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
 * 22.11.2005 | BNK | Erstellung
 * 26.06.2006 | BNK | +zoomFonts
 *                  | refak. von setLookAndFeel() zu setLookAndFeelOnce()
 * 27.07.2006 | BNK | "auto" Wert explizit parsen.
 * 29.07.2009 | BED | +configureTextFieldBehaviour()
 * 26.02.2010 | BED | +setWollMuxIcon(JFrame)
 * 17.04.2012 | AEK | +setLookAndFeel() um das konfigurierbare LAF erweitert
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 *
 */
package de.muenchen.mailmerge.dialog;

import java.awt.AWTEvent;
import java.awt.Font;
import java.awt.Image;
import java.awt.KeyEventPostProcessor;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.UIDefaults;
import javax.swing.UIManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.mailmerge.MailMergeFiles;

/**
 * Enthält von den Dialogen gemeinsam genutzten Code.
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class Common
{

  private static final Logger LOGGER = LoggerFactory.getLogger(Common.class);

  /**
   * Spezialwert wenn eine Breite oder Höhe die maximal sinnvolle sein soll.
   */
  public static final int DIMENSION_MAX = -1;

  /**
   * Spezialwert, wenn eine Breite oder Höhe nicht angegeben wurde.
   */
  public static final int DIMENSION_UNSPECIFIED = -2;

  /**
   * Spezialwert, wenn eine X oder Y Koordinate so gesetzt werden soll, dass das
   * Fenster in der Mitte positioniert ist.
   */
  public static final int COORDINATE_CENTER = -1;

  /**
   * Spezialwert wenn eine X oder Y Koordinate die maximal sinnvolle sein soll.
   */
  public static final int COORDINATE_MAX = -2;

  /**
   * Spezialwert wenn eine X oder Y Koordinate die minimal sinnvolle sein soll.
   */
  public static final int COORDINATE_MIN = -3;

  /**
   * Spezialwert, wenn eine X oder Y Koordinate nicht angegeben wurde.
   */
  public static final int COORDINATE_UNSPECIFIED = -4;

  /**
   * Der Unit-Increment für vertikale Scrollbars, die Zeilen von Eingabefeldern
   * enthalten, wie z,B, die Steuerelemente-Ansicht des FM4000.
   */
  private static int verticalScrollbarUnitIncrement = 12;

  /**
   * Gibt an ob {@link #setLookAndFeel()} bereits aufgerufen wurde.
   */
  private static boolean lafSet = false;

  private static boolean isPopupVisible = false;

  /**
   * Speichert die ursprünglichen Fontgrößen.
   */
  private static HashMap<Object, Float> defaultFontsizes;

  /**
   * Führt {@link #setLookAndFeel()} aus, aber nur, wenn es bisher noch nicht
   * ausgeführt wurde.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void setLookAndFeelOnce()
  {
    if (!lafSet)
      setLookAndFeel();
  }

  /**
   * Setzt, ob gerade ein Popup angezeigt wird.
   *
   * @param isPopupVisible
   */
  public static void setIsPopupVisible(boolean isPopupVisible) {
    Common.isPopupVisible = isPopupVisible;
  }

  /**
   * Setzt das Metal Look and Feel und ruft {@link #configureTextFieldBehaviour()}
   * auf. Das plattformspezifische LAF wird nicht verwendet, damit die Benutzer unter
   * Windows und Linux eine einheitliche Optik haben, so dass a) Schulungsvideos und
   * Unterlagen für beide Plattformen anwendbar sind und b) Benutzer sich bei der
   * Umstellung von Windows auf Linux nicht noch beim WollMux umgewöhnen müssen. Des
   * weiteren hatte zumindest als wir angefangen haben das GTK Look and Feel einige
   * Bugs. Es ist also auch ein Problem, dass wir nicht genug Ressourcen haben, um 2
   * Plattformen diesbzgl. zu testen und zu debuggen.
   *
   * Als Kompromiss ist es möglich das zu verwendende LAF über die
   * Konfiguration vorzugeben.
   *
   * alt: Setzt das System Look and Feel, falls es nicht MetalLookAndFeel ist.
   * Ansonsten setzt es GTKLookAndFeel falls möglich.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static void setLookAndFeel()
  {
    LOGGER.debug("setLookAndFeel");

    // Das Standard-LAF für den WollMux
    String lafName = "javax.swing.plaf.metal.MetalLookAndFeel";
    try
    {

      try
      {

        // Ist der Konfig-Parameter "LAF_CLASS_NAME" gesetzt, wird das angegebene
        // LAF verwendet.
        ConfigThingy config = MailMergeFiles.getWollmuxConf();
        ConfigThingy lafConf = config.get( "LAF_CLASS_NAME" );
        lafName = lafConf.toString();

      } // try
      catch ( Exception e )
      {} // catch

      UIManager.setLookAndFeel(lafName);
    }
    catch (Exception x)
    {}

    // JFrame.setDefaultLookAndFeelDecorated(true); seems to cause problems with
    // undecorated windows in Metal LAF

    // configure behaviour of JTextFields:
    configureTextFieldBehaviour();

    defaultFontsizes = new HashMap<>();
    UIDefaults def = UIManager.getLookAndFeelDefaults();
    Enumeration<Object> keys = def.keys();
    float fontSize;
    Object key;
    Font font;
    while (keys.hasMoreElements()){
      key = keys.nextElement();
      font = def.getFont(key);
      if (font != null){
        fontSize = font.getSize();
        defaultFontsizes.put(key, fontSize);
      }
    }
    lafSet = true;
  }

  /**
   * Konfiguriert das Verhalten von JTextFields, die in der GUI der Applikation
   * benutzt werden, so dass es dem erwarteten Standard-Verhalten von Textfeldern
   * entspricht - nämlich dass eventuell vorhandener Text im Textfeld automatisch
   * selektiert wird, wenn mit der Tabulator-Taste (nicht aber mit der Maus) in das
   * Feld gewechselt wird, so dass man einfach lostippen kann um den Inhalt zu
   * überschreiben.
   *
   * Dazu installieren wir im aktuellen {@link KeyboardFocusManager} einen
   * {@link KeyEventPostProcessor}, der beim Loslassen ({@link KeyEvent#KEY_RELEASED}
   * ) der Tabulator-Taste überprüft, ob das KeyEvent von einem JTextField ausgelöst
   * wurde und in diesem Fall allen Text in dem Textfeld selektiert.
   *
   * Sollte irgendwann mal RFE 4493590
   * (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4493590) umgesetzt werden,
   * kann man das ganze vielleicht besser lösen.
   *
   * Außerdem wird ein Swing-Problem korrigiert, durch das es vorkommen kann, dass in
   * einem JTextField selektierter Text auch nachdem des Textfeld den Focus verloren
   * hat noch als selektiert angezeigt wird.
   *
   * @author Daniel Benkmann (D-III-ITD-D101)
   */
  private static void configureTextFieldBehaviour()
  {
    // Hier wird konfiguriert das rechts Mouse click in ein TextField. Das Mouse listener
    // ist als AWTEventListener implementiert um eine globaler event listener zu haben,
    // er reagiert an eine event in alle Komponente.
    Toolkit.getDefaultToolkit().addAWTEventListener(new ContextMenuMouseListener(), AWTEvent.MOUSE_EVENT_MASK);

    KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();

    // Wir fügen dem aktuellen KeyboardFocusManager einen
    // KeyEventPostProcessor hinzu, der beim Loslassen (KeyEvent.KEY_RELEASED)
    // der Tabulator-Taste überprüft, ob das KeyEvent von einem JTextField ausgelöst
    // wurde und in diesem Fall allen Text in dem Textfeld selektiert.
    kfm.addKeyEventPostProcessor(event ->
    {
      if (event.getKeyCode() == KeyEvent.VK_TAB && event.getID() == KeyEvent.KEY_RELEASED
        && event.getComponent() instanceof JTextField)
      {
        JTextField textField = (JTextField) event.getComponent();
        textField.selectAll();
      }
      return false;
    });

    // Wir melden am aktuellen KeyboardFocusManager einen PropertyChangeListener an,
    // der bemerkt, ob ein JTextField den Focus verloren hat und dessen Selektion
    // löscht.
    // Das darf nicht passieren wenn in ein JTextField das recht Mouse Taste geklickt ist
    // und ein PopUp Menu angezeigt wird.
    kfm.addPropertyChangeListener("focusOwner", event ->
    {
      if (event.getOldValue() instanceof JTextField && !isPopupVisible)
      {
        JTextField textField = (JTextField) event.getOldValue();

        // Aufruf von setCaretPosition löscht die Selektion.
        textField.setCaretPosition(textField.getCaretPosition());
      }
    });
  }

  /**
   * Multipliziert alle Font-Größen mit zoomFactor. ACHTUNG! Nach jedem Aufruf von
   * setLookAndFeel() kann diese Funktion genau einmal verwendet werden und hat in
   * folgenden Aufrufen keine Wirkung mehr, bis wieder setLookAndFeel() aufgerufen
   * wird (was den Zoom wieder zurücksetzt).
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static void zoomFonts(double zoomFactor)
  {
    LOGGER.debug("zoomFonts({})", zoomFactor);
    UIDefaults def = UIManager.getLookAndFeelDefaults();
    Set<Map.Entry<Object, Float>> mappings;
    mappings = defaultFontsizes.entrySet();
    Iterator<Entry<Object, Float>> mappingEntries = mappings.iterator();
    Entry<Object,Float> mappingEntry;
    Object key;
    Float size;
    Font oldFnt;
    Font newFont;
    int changedFonts = 0;
    while (mappingEntries.hasNext())
    {
      try
      {
        mappingEntry = mappingEntries.next();
        key = mappingEntry.getKey();
        size = mappingEntry.getValue();
        oldFnt = def.getFont(key);
        if (oldFnt != null) {
          newFont = oldFnt.deriveFont((float)(size * zoomFactor));
          def.put(key, newFont);
          ++changedFonts;
        }
      }
      catch (Exception ex)
      {
        LOGGER.debug("", ex);
      }
    }

    LOGGER.debug(changedFonts + L.m(" Fontgrößen verändert!"));
  }

  public static int getVerticalScrollbarUnitIncrement()
  {
    return verticalScrollbarUnitIncrement;
  }

  /**
   * Parst WIDTH, HEIGHT, X und Y aus fensterConf und liefert ein entsprechendes
   * Rectangle. Spezialwerte wie {@link #COORDINATE_CENTER} und
   * {@link #DIMENSION_MAX} werden verwendet.
   *
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public static Rectangle parseDimensions(ConfigThingy fensterConf)
  {
    Rectangle r = new Rectangle();
    r.x = COORDINATE_UNSPECIFIED;
    try
    {
      String xStr = fensterConf.get("X").toString();
      if (xStr.equalsIgnoreCase("center"))
        r.x = COORDINATE_CENTER;
      else if (xStr.equalsIgnoreCase("max"))
        r.x = COORDINATE_MAX;
      else if (xStr.equalsIgnoreCase("min"))
        r.x = COORDINATE_MIN;
      else if (xStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.x = Integer.parseInt(xStr);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (r.x < 0)
          r.x = 0;
      }
    }
    catch (Exception x)
    {}

    r.y = COORDINATE_UNSPECIFIED;
    try
    {
      String yStr = fensterConf.get("Y").toString();
      if (yStr.equalsIgnoreCase("center"))
        r.y = COORDINATE_CENTER;
      else if (yStr.equalsIgnoreCase("max"))
        r.y = COORDINATE_MAX;
      else if (yStr.equalsIgnoreCase("min"))
        r.y = COORDINATE_MIN;
      else if (yStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.y = Integer.parseInt(yStr);
        // Ja, das folgende ist eine Einschränkung, aber
        // negative Koordinaten gehen in KDE eh nicht und kollidieren mit
        // obigen Festlegungen
        if (r.y < 0)
          r.y = 0;
      }
    }
    catch (Exception x)
    {}

    r.width = DIMENSION_UNSPECIFIED;
    try
    {
      String widthStr = fensterConf.get("WIDTH").toString();
      if (widthStr.equalsIgnoreCase("max"))
        r.width = DIMENSION_MAX;
      else if (widthStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.width = Integer.parseInt(widthStr);
        if (r.width < 0)
          r.width = DIMENSION_UNSPECIFIED;
      }
    }
    catch (Exception x)
    {}

    r.height = DIMENSION_UNSPECIFIED;
    try
    {
      String heightStr = fensterConf.get("HEIGHT").toString();
      if (heightStr.equalsIgnoreCase("max"))
        r.height = DIMENSION_MAX;
      else if (heightStr.equalsIgnoreCase("auto"))
      {/* nothing */}
      else
      {
        r.height = Integer.parseInt(heightStr);
        if (r.height < 0)
          r.height = DIMENSION_UNSPECIFIED;
      }
    }
    catch (Exception x)
    {}

    return r;
  }

  /**
   * Sets the icon of the passed in JFrame to the WollMux icon.
   *
   * @param myFrame
   *          JFrame which should get the WollMux icon
   * @author Daniel Benkmann
   */
  public static void setWollMuxIcon(JFrame myFrame)
  {
    try
    {
      URL url = Common.class.getClassLoader().getResource("data/wollmux_icon32x32.png");

      if (url == null)
      {
        LOGGER.error("Could not retrieve image from resource.");

        return;
      }

      Image image = Toolkit.getDefaultToolkit().createImage(url);
      myFrame.setIconImage(image);
    }
    catch (Throwable e)
    {
      LOGGER.error("", e);
    }
  }

  /**
   * Bricht jede Zeile aus text nach margin Zeichen beim nächsten Leerzeichen
   * um.
   *
   * @param text
   *          Der Text zum umbrechen.
   * @param margin
   *          Die Anzahl an Zeichen, nach denen umgebrochen werden soll.
   * @return Der umgebrochene Text.
   */
  public static String wrapLines(String text, int margin)
  {
    String formattedMessage = "";
    String[] lines = text.split("\n");
    for (int i = 0; i < lines.length; i++)
    {
      String[] words = lines[i].split(" ");
      int chars = 0;
      for (int j = 0; j < words.length; j++)
      {
        String word = words[j];
        if (margin > 0 && chars > 0 && chars + word.length() > margin)
        {
          formattedMessage += "\n";
          chars = 0;
        }
        formattedMessage += word + " ";
        chars += word.length() + 1;
      }
      if (i != lines.length - 1)
        formattedMessage += "\n";
    }
    return formattedMessage;
  }
}
