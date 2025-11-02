# Emergency Severity Assessment Feature

## Overview

The Emergency Severity Assessment feature enhances the CitiAlertsPH app by providing a structured
way to evaluate and prioritize emergency requests. After a user selects an emergency type, they are
presented with quick assessment questions that help determine the urgency and requirements for the
emergency response.

## User Flow

### 1. Emergency Request Initiation

1. User taps "REQUEST EMERGENCY HELP"
2. System displays emergency type selection dialog
3. User selects emergency type (Fire, Medical, Criminal Activity, Flood, Other)

### 2. Severity Assessment Dialog

After selecting emergency type, user is presented with assessment questions:

#### Question 1: Severity Level

- **Minor (manageable)** - Level 1-2
- **Moderate (needs assistance)** - Level 3
- **Severe (life-threatening)** - Level 4-5

#### Question 2: Injuries

- **Yes** - Increases severity level by 1
- **No** - No change to severity level

#### Question 3: Area Accessibility

- **Yes** - Area is accessible to responders
- **No** - Area has access difficulties (increases severity by 1)
- **Blocked** - Area is blocked/inaccessible (increases severity by 1)

#### Question 4: Additional Information (Optional)

- Free text field for additional context
- Helps responders prepare appropriate response

### 3. Severity Level Calculation

The system automatically calculates a final severity level (1-5) based on:

- Base severity selection
- Injury status (+1 if injuries reported)
- Accessibility issues (+1 if blocked/inaccessible)

**Examples:**

- Minor + No injuries + Accessible = Level 1
- Moderate + Injuries + Blocked = Level 5 (Critical)
- Severe + No injuries + Accessible = Level 4

## UI Components

### Severity Assessment Dialog

- **File:** `dialog_severity_assessment.xml`
- **Class:** `SeverityAssessmentDialog.java`
- Modern Material Design with card-based layout
- Dynamic button color changes based on severity selection
- Input validation to ensure all required questions are answered

### Emergency Details Dialog (Updated)

- **File:** `dialog_emergency_details.xml` (updated)
- Displays comprehensive severity information:
    - Severity level badge with color coding
    - Severity description (Minor/Moderate/Severe)
    - Injury status with icons
    - Accessibility status with icons
    - Additional notes (if provided)

### Emergency List Items (Enhanced)

- **File:** `item_emergency.xml` (updated)
- **Adapter:** `EmergencyListAdapter.java` (updated)
- Shows severity badges (L1, L2, L3, L4, L5)
- Color-coded severity indicators
- Injury and accessibility warning icons
- Severity descriptions in list view

## Data Model

### EmergencyRequest Class Updates

```java
// New fields added:
private int severityLevel;          // 1-5 scale
private String severityDescription; // "Minor", "Moderate", "Severe"
private boolean hasInjuries;       // true/false
private String areaAccessible;     // "Yes", "No", "Blocked"
private String additionalNotes;    // Optional text
```

### Helper Methods

- `getSeverityColor()` - Returns appropriate color for UI display
- `getSeverityIcon()` - Returns appropriate icon for severity level
- `calculateSeverityLevel()` - Static method for severity calculation

## Database Schema Changes

### New Columns Added to `emergency_requests` table:

```sql
severity_level INT DEFAULT 0
severity_description VARCHAR(20) DEFAULT NULL
has_injuries BOOLEAN DEFAULT FALSE
area_accessible VARCHAR(10) DEFAULT NULL
additional_notes TEXT DEFAULT NULL
```

### Indexes for Performance:

- `idx_emergency_severity` - On (severity_level, status)
- `idx_emergency_priority` - On (severity_level, has_injuries, status, created_at)

## API Integration

### Request Payload Updates

Emergency creation requests now include:

```json
{
  "user_id": 123,
  "emergency_type": "Fire",
  "latitude": 10.1234,
  "longitude": 120.5678,
  "location_name": "123 Main St",
  "severity_level": 4,
  "severity_description": "Severe",
  "has_injuries": true,
  "area_accessible": "Blocked",
  "additional_notes": "Building evacuation in progress"
}
```

## Responder Benefits

### Priority-Based Response

1. **Critical (Level 5)** - Immediate response dispatched
2. **High Priority (Level 4)** - Urgent response required
3. **Priority (Level 3)** - Standard priority response
4. **Normal (Level 1-2)** - Regular response time

### Enhanced Information

- Responders receive detailed situation assessment
- Better preparation for appropriate response
- Resource allocation optimization
- Improved safety for responders

### Visual Indicators

- Color-coded severity badges in emergency list
- Injury warnings with medical alert icons
- Accessibility warnings for route planning
- Additional context notes for better preparation

## UI Color Coding

### Severity Levels

- **Level 1-2 (Minor):** Green (#4CAF50)
- **Level 3 (Moderate):** Orange (#FF9800)
- **Level 4-5 (Severe/Critical):** Red (#F44336)

### Status Indicators

- **Injuries:** Red warning icon
- **Accessibility Issues:** Orange/Red location icon
- **Area Blocked:** Red blocked icon

## Implementation Files

### New Files Created:

1. `SeverityAssessmentDialog.java` - Assessment dialog handler
2. `dialog_severity_assessment.xml` - Assessment dialog layout
3. `update_emergency_severity_schema.sql` - Database update script
4. `EMERGENCY_SEVERITY_ASSESSMENT.md` - This documentation

### Modified Files:

1. `EmergencyRequest.java` - Added severity fields and methods
2. `EmergencyActivity.java` - Integrated assessment dialog
3. `dialog_emergency_details.xml` - Added severity display
4. `item_emergency.xml` - Added severity indicators
5. `EmergencyListAdapter.java` - Display severity information
6. `strings.xml` - Added severity-related string resources
7. `colors.xml` - Added severity color definitions

## Future Enhancements

### Potential Improvements:

1. **Photo Attachment** - Allow users to attach photos during assessment
2. **GPS Accuracy Indicator** - Show location accuracy in assessment
3. **Automatic Severity Detection** - Use emergency type to suggest initial severity
4. **Responder Feedback** - Allow responders to update severity on-site
5. **Historical Analytics** - Track severity accuracy and response times
6. **Multi-language Support** - Translate assessment questions
7. **Voice Input** - Allow voice notes for additional information
8. **Weather Integration** - Factor weather conditions into severity

### Advanced Features:

1. **Clustering** - Group nearby emergencies with similar severity
2. **Resource Matching** - Match emergency severity with appropriate resources
3. **Predictive Analytics** - Predict response times based on severity
4. **Emergency Escalation** - Automatic escalation for critical emergencies

## Testing Scenarios

### Test Cases:

1. **Complete Assessment Flow** - Test full user journey from emergency button to request sent
2. **Validation Testing** - Ensure all required fields are validated
3. **Severity Calculation** - Verify correct severity level calculation
4. **UI Display Testing** - Check proper display of severity information
5. **Responder View Testing** - Verify responder can see and respond to assessed emergencies
6. **Edge Cases** - Test with missing data, network issues, etc.

## Conclusion

The Emergency Severity Assessment feature significantly improves the emergency response system by:

- Providing structured emergency evaluation
- Enabling priority-based response
- Improving responder preparation
- Enhancing user experience with guided questions
- Supporting better resource allocation

This feature transforms the app from a basic emergency notification system to a comprehensive
emergency management platform that benefits both emergency reporters and responders.