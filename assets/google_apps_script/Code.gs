/**
 * Google Apps Script Web App Engine for Cricket Player Auction
 * Google Cloud & Google Sheets Database Handler
 * 
 * Instructions:
 * 1. Open Google Sheets (https://sheets.new)
 * 2. Go to Extensions -> Apps Script
 * 3. Replace Code.gs content with this code
 * 4. Click Deploy -> New deployment -> Web app
 * 5. Execute as: Me | Who has access: Anyone
 * 6. Copy Web App URL into Cricket App -> Settings -> Google Sheets Endpoint
 */

function doGet(e) {
  var action = (e && e.parameter && e.parameter.action) ? e.parameter.action : "getData";
  ensureDatabaseSchema();

  if (action === "getData") {
    return ContentService.createTextOutput(JSON.stringify(getCompleteDatabase()))
      .setMimeType(ContentService.MimeType.JSON);
  }

  return ContentService.createTextOutput(JSON.stringify({ status: "success", message: "Cricket Auction API Active" }))
    .setMimeType(ContentService.MimeType.JSON);
}

function doPost(e) {
  ensureDatabaseSchema();
  try {
    var data = JSON.parse(e.postData.contents);
    var action = data.action;

    if (action === "placeBid") {
      updatePlayerBid(data.playerId, data.teamId, data.amount, data.bidderEmail);
    } else if (action === "sellPlayer") {
      finalizePlayerSale(data.playerId, data.teamId, data.finalPrice);
    } else if (action === "syncData") {
      saveFullData(data);
    } else if (action === "createSheet") {
      var newSs = SpreadsheetApp.create(data.title || "Cricket_Auction_Database");
      return ContentService.createTextOutput(JSON.stringify({ status: "success", url: newSs.getUrl(), id: newSs.getId() }))
        .setMimeType(ContentService.MimeType.JSON);
    } else if (action === "exportDrive") {
      var file = DriveApp.createFile(data.filename || "Cricket_Auction_Backup.json", data.content || "{}", MimeType.PLAIN_TEXT);
      return ContentService.createTextOutput(JSON.stringify({ status: "success", url: file.getUrl(), id: file.getId() }))
        .setMimeType(ContentService.MimeType.JSON);
    }

    return ContentService.createTextOutput(JSON.stringify({ status: "success", data: getCompleteDatabase() }))
      .setMimeType(ContentService.MimeType.JSON);
  } catch (err) {
    return ContentService.createTextOutput(JSON.stringify({ status: "error", error: err.toString() }))
      .setMimeType(ContentService.MimeType.JSON);
  }
}

function getCompleteDatabase() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  var players = sheetToObjects(ss.getSheetByName("Players"));
  var teams = sheetToObjects(ss.getSheetByName("Teams"));
  var tournaments = sheetToObjects(ss.getSheetByName("Tournaments"));
  var bids = sheetToObjects(ss.getSheetByName("Bids"));

  return {
    status: "success",
    timestamp: new Date().getTime(),
    players: players,
    teams: teams,
    tournaments: tournaments,
    bids: bids
  };
}

function sheetToObjects(sheet) {
  if (!sheet) return [];
  var data = sheet.getDataRange().getValues();
  if (data.length < 2) return [];

  var headers = data[0];
  var result = [];

  for (var i = 1; i < data.length; i++) {
    var obj = {};
    for (var j = 0; j < headers.length; j++) {
      var val = data[i][j];
      if (val === "true") val = true;
      if (val === "false") val = false;
      obj[headers[j]] = val;
    }
    result.push(obj);
  }
  return result;
}

function updatePlayerBid(playerId, teamId, amount, bidderEmail) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Players");
  var data = sheet.getDataRange().getValues();
  
  for (var i = 1; i < data.length; i++) {
    if (data[i][0] == playerId) {
      sheet.getRange(i + 1, 7).setValue(amount); // currentBid column
      sheet.getRange(i + 1, 8).setValue(teamId); // highestBidderTeamId column
      sheet.getRange(i + 1, 9).setValue("BIDDING"); // status column
      break;
    }
  }

  // Record Bid
  var bidsSheet = ss.getSheetByName("Bids");
  bidsSheet.appendRow(["bid_" + new Date().getTime(), playerId, teamId, amount, bidderEmail || "", new Date().toISOString()]);
}

function finalizePlayerSale(playerId, teamId, finalPrice) {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  var sheet = ss.getSheetByName("Players");
  var data = sheet.getDataRange().getValues();

  for (var i = 1; i < data.length; i++) {
    if (data[i][0] == playerId) {
      sheet.getRange(i + 1, 7).setValue(finalPrice); // currentBid
      sheet.getRange(i + 1, 8).setValue(teamId); // highestBidderTeamId
      sheet.getRange(i + 1, 9).setValue("SOLD"); // status
      sheet.getRange(i + 1, 10).setValue(finalPrice); // soldPrice
      sheet.getRange(i + 1, 11).setValue(teamId); // soldToTeamId
      break;
    }
  }
}

function ensureDatabaseSchema() {
  var ss = SpreadsheetApp.getActiveSpreadsheet();
  
  // Players Sheet
  if (!ss.getSheetByName("Players")) {
    var pSheet = ss.insertSheet("Players");
    pSheet.appendRow(["id", "tournamentId", "name", "role", "country", "isOverseas", "basePrice", "currentBid", "highestBidderTeamId", "status", "soldPrice", "soldToTeamId", "imageUrl", "stats", "setName"]);
  }

  // Teams Sheet
  if (!ss.getSheetByName("Teams")) {
    var tSheet = ss.insertSheet("Teams");
    tSheet.appendRow(["id", "tournamentId", "name", "shortCode", "primaryColorHex", "logoUrl", "totalPurse", "spentPurse", "captainEmail", "inviteCode"]);
  }

  // Tournaments Sheet
  if (!ss.getSheetByName("Tournaments")) {
    var trSheet = ss.insertSheet("Tournaments");
    trSheet.appendRow(["id", "name", "defaultPurse", "maxSlots", "maxOverseas", "driveFileUrl"]);
    trSheet.appendRow(["t1", "Global Cricket Champions League T20", 100.0, 25, 8, ""]);
  }

  // Bids Sheet
  if (!ss.getSheetByName("Bids")) {
    var bSheet = ss.insertSheet("Bids");
    bSheet.appendRow(["id", "playerId", "teamId", "amount", "bidderEmail", "timestamp"]);
  }
}
