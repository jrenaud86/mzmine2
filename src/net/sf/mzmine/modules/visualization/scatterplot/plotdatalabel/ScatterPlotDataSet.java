/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.visualization.scatterplot.plotdatalabel;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.util.Range;

import org.jfree.data.xy.AbstractXYDataset;

public class ScatterPlotDataSet extends AbstractXYDataset{

	private PeakList peakList;
	private int domainX, domainY;
	private Integer[][] arraySeriesAndItemsSelection;
	private Color[] seriesColor;
	private RawDataFile[] rawDataFiles;
	private ActionListener visualizer;
	private int presentOnlyX, presentOnlyY;

	public ScatterPlotDataSet(PeakList peakList, int domainX, int domainY,
			ActionListener visualizer) {

		this.peakList = peakList;
		this.domainX = domainX;
		this.domainY = domainY;
		this.visualizer = visualizer;
		rawDataFiles = peakList.getRawDataFiles();
		seriesColor = new Color[]{ Color.BLUE };

		// Initialize and array which contains the row's index where is present both peaks
		initializeArraySeriesAndItems();


	}

	/**
	 * 
	 */
	private void initializeArraySeriesAndItems() {

		PeakListRow row;
		ChromatographicPeak peakX, peakY;
		Vector<Integer> peakRowIds = new Vector<Integer>();
		presentOnlyX = 0;
		presentOnlyY = 0;

		// Verifies that both peaks exist
		for (int i = 0; i < peakList.getNumberOfRows(); i++) {
			row = peakList.getRow(i);
			peakX = row.getPeak(rawDataFiles[domainX]);
			peakY = row.getPeak(rawDataFiles[domainY]);

			if ((peakX != null) && (peakY != null)) {
				peakRowIds.add(i);
			}
			if ((peakX != null) && (peakY == null)) {
				presentOnlyX++;
			}
			if ((peakX == null) && (peakY != null)) {
				presentOnlyY++;
			}
		}
		
		Integer[] listOfIds = peakRowIds.toArray(new Integer[0]);
		arraySeriesAndItemsSelection = new Integer[1][peakRowIds.size()];
		arraySeriesAndItemsSelection[0] = listOfIds;

	}

	@Override
	public int getSeriesCount() {
		return arraySeriesAndItemsSelection.length;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		return 1;
	}

	/**
	 * 
	 */
	public int getItemCount(int series) {
		return arraySeriesAndItemsSelection[series].length;
	}

	/**
	 * 
	 */
	public Number getX(int series, int item) {
		int rowID = arraySeriesAndItemsSelection[series][item];
		return peakList.getRow(rowID).getPeak(rawDataFiles[domainX])
				.getHeight();
	}

	/**
	 * 
	 */
	public Number getY(int series, int item) {
		int rowID = arraySeriesAndItemsSelection[series][item];
		return peakList.getRow(rowID).getPeak(rawDataFiles[domainY])
				.getHeight();
	}

	/**
	 * 
	 * @param series
	 * @param item
	 * @return
	 */
	public int getArrayIndex(int series, int item) {
		int index = arraySeriesAndItemsSelection[series][item];
		return index;
	}

	/**
	 * 
	 * @return
	 */
	public PeakList getPeakList() {
		return peakList;
	}

	/**
	 * 
	 * @param domainX
	 * @param domainY
	 */
	public void setDomainsIndexes(int domainX, int domainY) {
		this.domainX = domainX;
		this.domainY = domainY;
		initializeArraySeriesAndItems();
	}

	/**
	 * 
	 * @return int[]
	 */
	public int[] getDomainsIndexes() {
		int[] domains = new int[2];
		domains[0] = domainX;
		domains[1] = domainY;
		return domains;
	}

	/**
	 * 
	 * @return
	 */
	public String[] getDomainsNames() {
		Vector<String> domainsNames = new Vector<String>();
		for (RawDataFile file : rawDataFiles) {
			domainsNames.add(file.getName());
		}
		return domainsNames.toArray(new String[0]);
	}

	/**
	 * 
	 * @param index
	 * @return
	 */
	public String getDataPointName(int index) {
		
		PeakListRow row = peakList.getRow(index);
		PeakIdentity identity = row.getPreferredCompoundIdentity();
		if (identity != null) {
			return identity.getName();
		} else {
			return row.toString();
		}
	}

	/**
	 * 
	 * @param series
	 * @param item
	 * @return
	 */
	public String getDataPointName(int series, int item) {
		int rowID = arraySeriesAndItemsSelection[series][item];
		return peakList.getRow(rowID).getPreferredCompoundIdentity().getName();
	}


	/**
	 * Returns index of data point which exactly matches given X and Y values
	 * 
	 * @param domainX
	 * @param domainY
	 * @return
	 */
	public int getIndex(float valueX, float valueY) {
		
		Integer[] items = arraySeriesAndItemsSelection[0];

		for (int i = 0; i < items.length; i++) {
			
			double originalValueX = peakList.getRow(items[i]).getPeak(rawDataFiles[domainX])
			.getHeight();
			double originalValueY = peakList.getRow(items[i]).getPeak(rawDataFiles[domainY])
			.getHeight();

			if ((Math.abs(valueX - originalValueX) < 0.0000001f)
					&& (Math.abs(valueY - originalValueY) < 0.0000001f))
				return items[i];
		}
		return -1;
	}

	/**
	 * 
	 * @return
	 */
	public double[] getMaxMinValue() {

		Integer[] items = arraySeriesAndItemsSelection[0];

		int length = items.length;
		double valueX, valueY;
		double maxValue = 0, minValue = Double.MAX_VALUE;
		double[] maxMinValue = new double[2];
		for (int i = 0; i < length; i++) {

			valueX = peakList.getRow(items[i]).getPeak(rawDataFiles[domainX])
			.getHeight();

			valueY = peakList.getRow(items[i]).getPeak(rawDataFiles[domainY])
			.getHeight();

			if ((valueX > maxValue) || (valueY > maxValue)) {
				if (valueX > valueY)
					maxValue = valueX;
				else
					maxValue = valueY;
			}
			if ((valueX < minValue) || (valueY < minValue)) {
				if (valueX < valueY)
					minValue = valueX;
				else
					minValue = valueY;
			}
		}

		maxMinValue[0] = minValue;
		maxMinValue[1] = maxValue;

		return maxMinValue;
	}

	/**
	 * 
	 * @param series
	 * @return
	 */
	public Color getRendererColor(int series) {
		return seriesColor[series];
	}

	/**
	 * 
	 * @param searchValue
	 */
	public void updateListofAppliedSelection(String searchValue) {

		if (searchValue == null)
			return;

		
		Vector<Integer> items = new Vector<Integer>();
		Vector<Color> colorVector = new Vector<Color>();
		Integer[][] arraySeriesAndItemsConstruction;
		
		colorVector.add(Color.BLUE);

		Integer[] listOfIds = arraySeriesAndItemsSelection[0];
		int length1 = listOfIds.length;
		String selectionElement, originalElement;
		PeakListRow row;
		PeakIdentity identity;
		
		selectionElement = searchValue;
		selectionElement = selectionElement.toUpperCase();
		for (int rowID = 0; rowID < length1; rowID++) {
			
			originalElement = null;
			row = peakList.getRow(listOfIds[rowID]);
			identity = row.getPreferredCompoundIdentity();
			if (identity != null) {
				originalElement = identity.getName();
			}
			
			if ((originalElement == null) || (originalElement == "")) {
				continue;
			}
			
			originalElement = originalElement.toUpperCase();
			
			if ((originalElement.matches(".*" + selectionElement + ".*"))) {
				if (!items.contains(listOfIds[rowID]))
					items.add(listOfIds[rowID]);
			}
				
		}
			
	
		if (items.size() > 0){
			colorVector.add(Color.ORANGE);
			arraySeriesAndItemsConstruction = new Integer[2][0];
			arraySeriesAndItemsConstruction[0] = arraySeriesAndItemsSelection[0];
			arraySeriesAndItemsConstruction[1] = items.toArray(new Integer[0]);
			arraySeriesAndItemsSelection = arraySeriesAndItemsConstruction;
		}
		else{
			arraySeriesAndItemsConstruction = new Integer[1][0];
			arraySeriesAndItemsConstruction[0] = arraySeriesAndItemsSelection[0];
			arraySeriesAndItemsSelection = arraySeriesAndItemsConstruction;
		}

		seriesColor =colorVector.toArray(new Color[0]);
		visualizer.actionPerformed(new ActionEvent(this,
				ActionEvent.ACTION_PERFORMED, "DATASET_UPDATED"));


	}

	/**
	 * 
	 * @return String
	 */
	public String getDisplayedCount(){
		String display = "<HTML>" +  peakList.getNumberOfRows() + " total peaks, ";
		display += this.getItemCount(0) + " displayed<BR> ";
		display += presentOnlyX + " peaks only in " + rawDataFiles[domainX].getName() + "<BR>";
		display += presentOnlyY + " peaks only in " + rawDataFiles[domainY].getName();
		return  display;
	}

	/**
	 * 
	 * @param index
	 * @return peak[]
	 */
	public ChromatographicPeak[] getPeaksAt(int index) {
		ChromatographicPeak[] peaks = new ChromatographicPeak[2];
		peaks[0] = peakList.getPeak(index, rawDataFiles[domainX]);
		peaks[1] = peakList.getPeak(index, rawDataFiles[domainY]);

		return peaks;
	}

	/**
	 * 
	 * @param index
	 * @return range
	 */
	public Range getRawDataFilesRTRangeAt(int index) {
		Range rtRange = rawDataFiles[domainX].getDataRTRange(1);
		rtRange.extendRange(rawDataFiles[domainY].getDataRTRange(1));
		return rtRange;
	}

	/**
	 * 
	 * @param index
	 * @return range
	 */
	public Range getRawDataFilesMZRangeAt(int index) {
		Range mzRange = rawDataFiles[domainX].getDataMZRange(1);
		mzRange.extendRange(rawDataFiles[domainY].getDataMZRange(1));
		return mzRange;
	}
	
	public RawDataFile[] getRawDataFilesDisplayed(){
		RawDataFile[] files = new RawDataFile[] { rawDataFiles[domainX] , rawDataFiles[domainY] };
		return files;
	}

}