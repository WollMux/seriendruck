package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.HashSet;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import de.muenchen.allg.itd51.wollmux.core.dialog.DimAdjust;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

/**
 * Repräsentiert ein mit {@link UIElementType} beschriebenes Formularelement und
 * kann sichtbar/unsichtbar und aktiviert/nicht aktiviert sein. Außerdem kann es in
 * der Methode {@link UIElement#addSubmitArgs(Map)} vor einem Submit prüfen, ob die
 * Formularinhalte plausibel sind und entsprechend gültige Werte in die
 * Argumentliste aufnehmen oder eine {@link InvalidArgumentException}-Exception
 * werfen.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class UIElement
{
  private Component compo;

  private String group;

  private boolean visible = true;

  private boolean enabled = true;

  /**
   * Enthält die Hintergrundfarbe des Beschreibungsfeldes im Druckdialog
   */
  public static final Color DESC_COLOR = new Color(0xffffc8);

  /**
   * Erzeugt ein UIElement, das über die Hauptkomponente compo dargestellt werden,
   * über die Sichtbarkeitsgruppe group ein-/ausgeblendet werden kann und im
   * Kontext vom {@link MailMergeParams} mmp gültig ist.
   */
  public UIElement(Component compo, String group)
  {
    this.compo = compo;
    this.group = group;
  }

  /**
   * Passt die Sichtbarkeit abhängig von den aktuell gesetzten Sichtbarkeitsgruppen
   * aus mmp.visibleGroups an.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void updateView(HashSet<String> visibleGroups)
  {
    if (group != null)
    {
      visible = visibleGroups.contains(group);
      compo.setVisible(visible);
    }
  }

  /**
   * Liefert die JComponent dieses UIElements zurück, wobei es sein kann, dass das
   * UIElement aus mehreren JComponents zusammengesetzt ist (in diesem Fall liefert
   * diese Methode nur die oberste JComponent wie z.B. eine HBox zurück).
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public Component getCompo()
  {
    return compo;
  }

  /**
   * Gibt Auskunft, ob das UIElement aktuell sichtbar ist.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public boolean isVisible()
  {
    return visible;
  }

  /**
   * Setzt den Aktiviert-Status des UIElements auf enabled.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void setEnabled(boolean enabled)
  {
    this.enabled = enabled;
    compo.setEnabled(enabled);
  }

  /**
   * Gibt Auskunft, ob das UIElement aktuell aktiviert ist.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public boolean isEnabled()
  {
    return enabled;
  }

  /**
   * Prüft die Benutzereingaben des UIElements und fügt die für eine Submit-Aktion
   * relevanten Benutzereingaben zu der Argumentliste args hinzu, die eine interne
   * Plausibilitätsprüfung bestanden haben. Bei nicht bestandener
   * Plausibilitätsprüfung wird eine {@link InvalidArgumentException} geschmissen.
   * 
   * @throws InvalidArgumentException
   *           Bei nicht bestandener Plausibilitätsprüfung
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {}

  /**
   * Fabrik-Methode für die Erzeugung aller {@link UIElement}-Objekte. Die erwarteten
   * String-Argumente können (je nach Formularelement) auch null sein, die anderen
   * Typen müssen mit Objekten != null belegt sein.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public static UIElement createUIElement(UIElementType type, String label,
      String labelFrom, String labelTo, UIElementAction action, String value,
      String group, Orientation orient, MailMergeParams mmp, Section section)
  {
    switch (type)
    {
      case label:
      {
        return new UIElement(new JLabel(label), group);
      }
  
      case radio:
      {
        return new RadioButtonUIElement(label, action, value, group, mmp);
      }
  
      case description:
      {
        JTextArea tf = new JTextArea(3, 50);
        tf.setEditable(false);
        tf.setFocusable(false);
        DimAdjust.fixedSize(tf);
        tf.setBackground(UIElement.DESC_COLOR);
        tf.setBorder(new LineBorder(UIElement.DESC_COLOR, 4));
        mmp.descriptionFields.add(tf);
        return new UIElement(tf, group);
      }
  
      case fromtoradio:
      {
        return new FromToRadioUIElement(labelFrom, labelTo, action, value, group,
          mmp);
      }
  
      case glue:
      {
        if (orient == Orientation.vertical)
          return new UIElement(Box.createVerticalGlue(), group);
        else
          return new UIElement(Box.createHorizontalGlue(), group);
      }
  
      case button:
      {
        JButton button = new JButton(label);
        button.addActionListener(action.createActionListener(value, mmp));
        return new UIElement(button, group);
      }
  
      case targetdirpicker:
      {
        return new TargetDirPickerUIElement(label, action, value, group, mmp);
      }
  
      case filenametemplatechooser:
      {
        // liefert einen filenamen *ohne* endung
        String name = mmp.getMMC().getDefaultFilename();
        JTextField textField = new JTextField(name);
        TextWithDatafieldTagsUIElement el =
          new TextWithDatafieldTagsUIElement(textField, textField,
            SubmitArgument.filenameTemplate,
            L.m("Sie müssen ein Dateinamenmuster angeben!"), group, mmp);
        el.getTextTags().getJTextComponent().setCaretPosition(name.length());
        el.getTextTags().insertTag(MailMergeParams.TAG_DATENSATZNUMMER);
        return el;
      }
  
      case emailtext:
      {
        JTextArea tf =
          new JTextArea(
            L.m("Sehr geehrte Damen und Herren,\n\nanbei erhalten Sie ...\n\nMit freundlichen Grüßen\n..."));
        JScrollPane sc = new JScrollPane(tf);
        TextWithDatafieldTagsUIElement el =
          new TextWithDatafieldTagsUIElement(tf, sc, SubmitArgument.emailText, null,
            group, mmp);
        return el;
      }
  
      case emailsubject:
      {
        return new EMailSubject(label, action, value, group, mmp);
      }
  
      case emailtofieldname:
      {
        return new EMailToFieldNameUIElement(label, action, value, group, mmp);
      }
  
      case emailfrom:
      {
        return new EMailFromUIElement(label, action, value, group, mmp);
      }
  
      case printersettings:
      {
        return new PrinterSettingsUIElement(label, group, mmp);
      }
      
      case unknown:
        break;
      default:
        break;
    }
    return null;
  }
}