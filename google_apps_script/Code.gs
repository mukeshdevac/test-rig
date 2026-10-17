/**
 * LOF TITAN S3 EOL TEST FIXTURE - GOOGLE APPS SCRIPT WEB APP BACKEND
 * 
 * Instructions for Deployment:
 * 1. Open Google Sheets -> Extensions -> Apps Script.
 * 2. Paste this entire file into Code.gs.
 * 3. Run setupSheetHeaders() once to initialize headers & styling.
 * 4. Click "Deploy" -> "New deployment".
 * 5. Select type: "Web app".
 *    - Description: "LOF Titan EOL Logger"
 *    - Execute as: "Me"
 *    - Who has access: "Anyone" (allows tablet WorkManager HTTP POST without OAuth token expiration).
 * 6. Copy the Web App URL and paste it into the Android App Settings Screen.
 */

function doPost(e) {
  var lock = LockService.getScriptLock();
  // Prevent concurrent write collisions from multiple factory tablets
  lock.tryLock(10000);

  try {
    var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
    
    // Ensure header row exists
    if (sheet.getLastRow() === 0) {
      setupSheetHeaders();
    }

    if (!e || !e.postData || !e.postData.contents) {
      return ContentService.createTextOutput(JSON.stringify({
        status: "ERROR",
        message: "No POST body received."
      })).setMimeType(ContentService.MimeType.JSON);
    }

    var payload = JSON.parse(e.postData.contents);

    var timestamp = payload.timestamp ? new Date(payload.timestamp) : new Date();
    var testId = payload.testId || "";
    var operatorId = payload.operatorId || "UNKNOWN";
    var serialNumber = payload.serialNumber || "UNKNOWN";
    var overallResult = (payload.overallResult || "FAIL").toUpperCase();
    var durationMs = payload.durationMs || 0;
    var quiescentCurrentMa = payload.quiescentCurrentMa || 0.0;
    var peakCurrentMa = payload.peakCurrentMa || 0.0;
    var busVoltageV = payload.busVoltageV || 0.0;
    var rawDiagnostics = payload.diagnosticsJson || "{}";

    // Append test record row
    sheet.appendRow([
      timestamp,
      testId,
      operatorId,
      serialNumber,
      overallResult,
      durationMs,
      quiescentCurrentMa,
      peakCurrentMa,
      busVoltageV,
      rawDiagnostics
    ]);

    var lastRow = sheet.getLastRow();
    
    // Color-code Overall Result column (Column 5)
    var resultCell = sheet.getRange(lastRow, 5);
    if (overallResult === "PASS") {
      resultCell.setBackground("#E8F5E9").setFontColor("#1B5E20").setFontWeight("bold");
    } else {
      resultCell.setBackground("#FFEBEE").setFontColor("#B71C1C").setFontWeight("bold");
    }

    return ContentService.createTextOutput(JSON.stringify({
      status: "SUCCESS",
      message: "Row appended successfully",
      row: lastRow,
      serialNumber: serialNumber,
      result: overallResult
    })).setMimeType(ContentService.MimeType.JSON);

  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({
      status: "ERROR",
      message: err.toString()
    })).setMimeType(ContentService.MimeType.JSON);

  } finally {
    lock.releaseLock();
  }
}

/**
 * Run this function once from the Apps Script editor to create styled headers.
 */
function setupSheetHeaders() {
  var sheet = SpreadsheetApp.getActiveSpreadsheet().getActiveSheet();
  sheet.setName("LOF_Titan_EOL_Logs");

  var headers = [
    "Timestamp",
    "Test UUID",
    "Operator ID",
    "PCB Serial Number",
    "Overall Result",
    "Duration (ms)",
    "Quiescent Current (mA)",
    "Peak Current (mA)",
    "Bus Voltage (V)",
    "Diagnostics Raw JSON"
  ];

  sheet.getRange(1, 1, 1, headers.length)
    .setValues([headers])
    .setFontWeight("bold")
    .setBackground("#212121")
    .setFontColor("#00E676")
    .setHorizontalAlignment("center");

  sheet.setFrozenRows(1);
  sheet.autoResizeColumns(1, headers.length);
}
