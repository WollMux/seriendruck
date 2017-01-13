package de.muenchen.mailmerge.dialog.mailmerge.gui;

import java.util.ArrayList;
import java.util.List;

import de.muenchen.mailmerge.dialog.mailmerge.DatasetSelectionType;

public class IndexSelection
{
  /**
   * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
   * bestimmt dies den ersten zu druckenden Datensatz (wobei der erste Datensatz
   * die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder größer als
   * {@link #rangeEnd} sein. Dies muss dort behandelt werden, wo er verwendet wird.
   */
  private int rangeStart = 1;

  /**
   * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#RANGE}
   * bestimmt dies den letzten zu druckenden Datensatz (wobei der erste Datensatz
   * die Nummer 1 hat). ACHTUNG! Der Wert hier kann 0 oder kleiner als
   * {@link #rangeStart} sein. Dies muss dort behandelt werden, wo er verwendet
   * wird.
   */
  private int rangeEnd = Integer.MAX_VALUE;

  /**
   * Falls {@link #datasetSelectionType} == {@link DatasetSelectionType#INDIVIDUAL}
   * bestimmt dies die Indizes der ausgewählten Datensätze, wobei 1 den ersten
   * Datensatz bezeichnet.
   */
  private List<Integer> selectedIndexes = new ArrayList<Integer>();

  public int getRangeStart()
  {
    return rangeStart;
  }

  public void setRangeStart(int rangeStart)
  {
    this.rangeStart = rangeStart;
  }

  public int getRangeEnd()
  {
    return rangeEnd;
  }

  public void setRangeEnd(int rangeEnd)
  {
    this.rangeEnd = rangeEnd;
  }

  public List<Integer> getSelectedIndexes()
  {
    return selectedIndexes;
  }

  public void setSelectedIndexes(List<Integer> selectedIndexes)
  {
    this.selectedIndexes = selectedIndexes;
  }

}