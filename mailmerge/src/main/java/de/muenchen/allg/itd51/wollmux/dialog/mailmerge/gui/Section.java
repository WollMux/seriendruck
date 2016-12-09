package de.muenchen.allg.itd51.wollmux.dialog.mailmerge.gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.border.Border;

import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.dialog.mailmerge.MailMergeParams;

/**
 * Beschreibt eine Section im Seriendruckdialog (z.B. "Aktionen" oder "Output") und
 * enthält UIElemente. Sind alle UIElement dieser Section unsichtbar, so ist auch
 * die Sektion selbst unsichtbar. Besitzt die Section einen TITLE ungleich null
 * oder Leerstring, so wird die Section mit einer TitledBorder verziert, ansonsten
 * nicht. Radio-Buttons erhalten innerhalb einer Section die selbe ButtonGroup,
 * weshalb für jede neue Gruppe eine neue Section erstellt werden muss.
 * 
 * @author Christoph Lutz (D-III-ITD-D101)
 */
public class Section
{
  List<UIElement> elements = new ArrayList<UIElement>();

  Box contentBox;

  boolean visible;

  /**
   * Erzeugt die Section, die über das ConfigThingy section beschrieben ist, in der
   * JComponent parent angezeigt werden soll und im Kontext des
   * MailMergeParams-Objekts mmp gültig ist.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public Section(ConfigThingy section, JComponent parent, MailMergeParams mmp)
  {
    String title = "";
    try
    {
      title = section.get("TITLE").toString();
    }
    catch (NodeNotFoundException e1)
    {}

    Orientation orient = Orientation.vertical;
    try
    {
      orient = Orientation.getByname(section.get("ORIENTATION").toString());
    }
    catch (NodeNotFoundException e1)
    {}

    Box hbox = Box.createHorizontalBox();
    parent.add(hbox);
    if (orient == Orientation.vertical)
      contentBox = Box.createVerticalBox();
    else
      contentBox = Box.createHorizontalBox();

    if (title.length() > 0)
    {
      Border border =
        BorderFactory.createTitledBorder(
          BorderFactory.createLineBorder(Color.GRAY), title);
      contentBox.setBorder(border);
    }
    else
    {
      if (orient == Orientation.vertical)
        contentBox.add(Box.createVerticalStrut(5));
    }
    hbox.add(contentBox);

    ConfigThingy elementsConf = section.queryByChild("TYPE");
    ButtonGroup buttonGroup = null;
    for (ConfigThingy element : elementsConf)
    {
      String label = element.getString("LABEL", null);
      String labelFrom = element.getString("LABEL_FROM", null);
      String labelTo = element.getString("LABEL_TO", null);
      UIElementType type =
        UIElementType.getByname(element.getString("TYPE", null));
      UIElementAction action =
        UIElementAction.getByname(element.getString("ACTION", null));
      String value = element.getString("VALUE", null);
      String group = element.getString("GROUP", null);
      UIElement uiel =
        UIElement.createUIElement(type, label, labelFrom, labelTo, action, value, group,
          orient, mmp, this);
      if (uiel != null)
      {
        elements.add(uiel);
        contentBox.add(uiel.getCompo());
        if (uiel instanceof HasRadioElement)
        {
          if (buttonGroup == null) buttonGroup = new ButtonGroup();
          ((HasRadioElement) uiel).setButtonGroup(buttonGroup);
        }
      }
    }
    if (orient == Orientation.vertical)
      contentBox.add(Box.createVerticalStrut(5));
  }

  /**
   * Aktualisiert alle in der Section enthaltenen UIElemente (bezüglich ihrer
   * Sichtbarkeit und Aktiviertheit) und passt ggf. die Voreinstellungen von allen
   * UIElementen an, die {@link HasRadioElement} implementieren.
   * 
   * @author Christoph Lutz (D-III-ITD-D101)
   */
  public void updateView(HashSet<String> visibleGroups)
  {
    visible = false;
    HasRadioElement firstEnabledRadio = null;
    boolean hasEnabledPreset = false;
    for (UIElement el : elements)
    {
      el.updateView(visibleGroups);
      if (el.isVisible()) visible = true;

      // ggf. Voreinstellungen von Radio-Buttons anpassen
      if (!(el instanceof HasRadioElement)) continue;
      HasRadioElement hre = (HasRadioElement) el;
      if (el.isVisible() && el.isEnabled())
      {
        if (firstEnabledRadio == null) firstEnabledRadio = hre;
        if (hre.isSelected()) hasEnabledPreset = true;
      }
    }

    if (!hasEnabledPreset && firstEnabledRadio != null)
      firstEnabledRadio.setSelected(true);

    contentBox.setVisible(visible);
  }

  public void addSubmitArgs(Map<SubmitArgument, Object> args)
      throws InvalidArgumentException
  {
    for (UIElement el : elements)
    {
      if (el.isVisible() && el.isEnabled()) el.addSubmitArgs(args);
    }
  }
}