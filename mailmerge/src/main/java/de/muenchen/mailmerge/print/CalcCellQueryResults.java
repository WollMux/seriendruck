package de.muenchen.mailmerge.print;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.util.L;

class CalcCellQueryResults implements QueryResults
{
  /**
   * Bildet einen Spaltennamen auf den Index in dem zu dem Datensatz gehörenden
   * String[]-Array ab.
   */
  private Map<String, Integer> mapColumnNameToIndex;

  private List<Dataset> datasets = new ArrayList<>();

  @Override
  public int size()
  {
    return datasets.size();
  }

  @Override
  public Iterator<Dataset> iterator()
  {
    return datasets.iterator();
  }

  @Override
  public boolean isEmpty()
  {
    return datasets.isEmpty();
  }

  public void setColumnNameToIndexMap(Map<String, Integer> mapColumnNameToIndex)
  {
    this.mapColumnNameToIndex = mapColumnNameToIndex;
  }

  public void addDataset(String[] data)
  {
    datasets.add(new MyDataset(data));
  }

  private class MyDataset implements Dataset
  {
    private String[] data;

    public MyDataset(String[] data)
    {
      this.data = data;
    }

    @Override
    public String get(String columnName) throws ColumnNotFoundException
    {
      Number idx = mapColumnNameToIndex.get(columnName);
      if (idx == null)
        throw new ColumnNotFoundException(L.m("Spalte %1 existiert nicht!",
          columnName));
      return data[idx.intValue()];
    }

    @Override
    public String getKey()
    {
      return "key";
    }

  }

}