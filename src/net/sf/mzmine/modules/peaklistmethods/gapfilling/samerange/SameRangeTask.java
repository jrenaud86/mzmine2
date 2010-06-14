/*
 * Copyright 2006-2010 The MZmine 2 Development Team
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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.gapfilling.samerange;

import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.taskcontrol.Task;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.Range;
import net.sf.mzmine.util.ScanUtils;

class SameRangeTask implements Task {

	private Logger logger = Logger.getLogger(this.getClass().getName());

	private TaskStatus status = TaskStatus.WAITING;
	private String errorMessage;

	private PeakList peakList, processedPeakList;

	private String suffix;

	private int processedRows, totalRows;

	private SameRangeParameters parameters;

	SameRangeTask(PeakList peakList, SameRangeParameters parameters) {

		this.peakList = peakList;
		this.parameters = parameters;

		suffix = (String) parameters
				.getParameterValue(SameRangeParameters.suffix);

	}

	public void run() {

		logger.info("Started gap-filling " + peakList);

		status = TaskStatus.PROCESSING;

		// Get total number of rows
		totalRows = peakList.getNumberOfRows();

		// Get peak list columns
		RawDataFile columns[] = peakList.getRawDataFiles();

		// Create new peak list
		processedPeakList = new SimplePeakList(peakList + " " + suffix, columns);

		// Fill gaps in given column
		for (int row = 0; row < totalRows; row++) {

			// Canceled?
			if (status == TaskStatus.CANCELED)
				return;

			PeakListRow sourceRow = peakList.getRow(row);
			PeakListRow newRow = new SimplePeakListRow(sourceRow.getID());

			// Copy comment
			newRow.setComment(sourceRow.getComment());

			// Copy identities
			for (PeakIdentity ident : sourceRow.getPeakIdentities())
				newRow.addPeakIdentity(ident, false);
			if (sourceRow.getPreferredPeakIdentity() != null)
				newRow.setPreferredPeakIdentity(sourceRow
						.getPreferredPeakIdentity());

			// Copy each peaks and fill gaps
			for (RawDataFile column : columns) {

				// Canceled?
				if (status == TaskStatus.CANCELED)
					return;

				// Get current peak
				ChromatographicPeak currentPeak = sourceRow.getPeak(column);

				// If there is a gap, try to fill it
				if (currentPeak == null)
					currentPeak = fillGap(sourceRow, column);

				// If a peak was found or created, add it
				if (currentPeak != null)
					newRow.addPeak(column, currentPeak);

			}

			processedPeakList.addRow(newRow);

			processedRows++;

		}

		// Append processed peak list to the project
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(processedPeakList);

		// Add task description to peakList
		processedPeakList
				.addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
						"Gap filling using RT and m/z range", parameters));

		status = TaskStatus.FINISHED;

		logger.info("Finished gap-filling " + peakList);

	}

	private ChromatographicPeak fillGap(PeakListRow row, RawDataFile column) {

		SameRangePeak newPeak = new SameRangePeak(column);

		Range mzRange = null, rtRange = null;

		// Check the peaks for selected data files
		for (RawDataFile dataFile : row.getRawDataFiles()) {
			ChromatographicPeak peak = row.getPeak(dataFile);
			if (peak == null)
				continue;
			if (mzRange == null) {
				mzRange = new Range(peak.getRawDataPointsMZRange());
				rtRange = new Range(peak.getRawDataPointsRTRange());
			} else {
				mzRange.extendRange(peak.getRawDataPointsMZRange());
				rtRange.extendRange(peak.getRawDataPointsRTRange());
			}
		}

		// Get scan numbers
		int[] scanNumbers = column.getScanNumbers(1, rtRange);

		boolean dataPointFound = false;

		for (int scanNumber : scanNumbers) {

			if (status == TaskStatus.CANCELED)
				return null;

			// Get next scan
			Scan scan = column.getScan(scanNumber);

			// Find most intense m/z peak
			DataPoint basePeak = ScanUtils.findBasePeak(scan, mzRange);

			if (basePeak != null) {
				if (basePeak.getIntensity() > 0)
					dataPointFound = true;
				newPeak.addDatapoint(scan.getScanNumber(), basePeak);
			} else {
				DataPoint fakeDataPoint = new SimpleDataPoint(mzRange
						.getAverage(), 0);
				newPeak.addDatapoint(scan.getScanNumber(), fakeDataPoint);
			}

		}

		if (dataPointFound) {
			newPeak.finalizePeak();
			if (newPeak.getArea() == 0)
				return null;
			return newPeak;
		}

		return null;
	}

	public void cancel() {
		status = TaskStatus.CANCELED;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public double getFinishedPercentage() {
		if (totalRows == 0)
			return 0;
		return (double) processedRows / (double) totalRows;

	}

	public TaskStatus getStatus() {
		return status;
	}

	public String getTaskDescription() {
		return "Gap filling " + peakList + " using RT and m/z range";
	}

	public Object[] getCreatedObjects() {
		return new Object[] { processedPeakList };
	}

}