package edu.pitt.dbmi.tutor.messages;

import java.awt.Color;
import java.awt.Dimension;


/**
 * this class describes several built-in constants that are used for messaging and help scenarios
 * @author tseytlin
 *
 */
public class Constants {
	public static final String DEBUG_PASSWORD = "secret";
	public static final String USER_INFO_FORGET_CASES_BEFORE = "ForgetCasesBefore";
	
	
	// message types
	public static final String TYPE_FINDING = "Finding";
	public static final String TYPE_ABSENT_FINDING = "AbsentFinding";
	public static final String TYPE_HYPOTHESIS = "Hypothesis";
	public static final String TYPE_DIAGNOSIS = "Diagnosis";
	public static final String TYPE_SUPPORT_LINK = "SupportLink";
	public static final String TYPE_REFUTE_LINK = "RefuteLink";
	public static final String TYPE_ATTRIBUTE = "Attribute";
	public static final String TYPE_ACTION = "Action";
	public static final String TYPE_RECOMMENDATION = "Recommendation";
	public static final String TYPE_HINT = "Hint";
	public static final String TYPE_HINT_LEVEL = "HintLevel";
	public static final String TYPE_DONE = "Done";
	public static final String TYPE_INFO = "Info";
	public static final String TYPE_QUESTION = "Question";
	public static final String TYPE_PRESENTATION = "TutorViewer";
	public static final String TYPE_TREE = "SelectionTree";
	public static final String TYPE_NODE = "Node";
	public static final String TYPE_BUTTON = "Button";
	public static final String TYPE_HYPERLINK = "HyperLink";
	public static final String TYPE_MENU_ITEM = "Menu Item";
	public static final String TYPE_DIALOG = "Dialog";
	public static final String TYPE_BOOK = "Book";
	public static final String TYPE_ANSWER = "Answer";
	public static final String TYPE_REPORT = "Report";
	public static final String TYPE_START = "Start";
	public static final String TYPE_END = "End";
	public static final String TYPE_DEBUG = "Debug";
	public static final String TYPE_TEXT = "Text";
	public static final String TYPE_CONCEPT = "Concept";
	
	// message actions
	public static final String ACTION_ADDED = "Add";
	public static final String ACTION_REMOVED = "Delete";
	public static final String ACTION_REFINE = "Refine";
	public static final String ACTION_REQUEST = "Request";
	public static final String ACTION_ASK = "Ask";
	public static final String ACTION_NEXT_STEP = "NextStep";
	public static final String ACTION_GLOSSARY = "Glossary";
	public static final String ACTION_SHOW_LOCATION = "ShowLocation";
	public static final String ACTION_INFERENCE = "Inference";
	public static final String ACTION_SELF_CHECK = "SelfCheck";
	public static final String ACTION_VIEW_CHANGE = "ViewChanged";
	public static final String ACTION_VIEW_RESIZE = "ViewResized";
	public static final String ACTION_IMAGE_CHANGE = "ImageChanged";
	public static final String ACTION_IMAGE_REQUEST = "ImageRequest";
	public static final String ACTION_IDENTIFY = "Identify Feature";
	public static final String ACTION_SEARCH   = "Search";
	public static final String ACTION_SEARCHING   = "Searching";
	public static final String ACTION_SELECTED = "Selected";
	public static final String ACTION_VIEW_EXAMPLE   = "ViewExample";
	public static final String ACTION_DESELECTED = "Deselected";
	public static final String ACTION_EXPANDED = "Expanded";
	public static final String ACTION_MOVED = "Moved";
	public static final String ACTION_MEASURE = "Measure";
	public static final String ACTION_OPENED = "Opened";
	public static final String ACTION_CLOSED = "Closed";
	public static final String ACTION_SUBMIT = "Submit";
	public static final String ACTION_SUMMARY = "Summary";
	public static final String ACTION_CLEAR = "Clear";
	
	// some common labels
	public static final String LABEL_NO = "NO";
	
	// tutor responses
	public static final String RESPONSE_CONFIRM = "Confirm";
	public static final String RESPONSE_FAILURE = "Failure";
	public static final String RESPONSE_HINT    = "Hint";
	// this response is not used, by a reasoner, but can be used
	// for fine-tuning feedback by FeedbackModule
	public static final String RESPONSE_IRRELEVANT = "Irrelevant";
	
	// link separators
	public static final String LINK_SUPPORT  = "_supports_";
	public static final String LINK_REFUTE   = "_refutes_";
	
	// potential outcomes
	public static final String OUTCOME_CLOSED = "closed";
	public static final String OUTCOME_FINISHED = "finished";
	public static final String OUTCOME_ERROR = "error";
	
	// case status
	public static final String STATUS_INCOMPLETE = "incomplete";
	public static final String STATUS_COMPLETE = "complete";
	public static final String STATUS_TESTED = "tested";

	// fok status
	public static final String CONCEPT_SURE = "sure";
	public static final String CONCEPT_UNSURE = "unsure";
	public static final String CONCEPT_ERROR = "error";
	public static final String CONCEPT_UNKNOWN = "unknown";
	
	// concept status
	public static final String CONCEPT_CORRECT = "correct";
	public static final String CONCEPT_INCORRECT = "incorrect";
	public static final String CONCEPT_IRRELEVANT = "irrelevant";
	
	// concept size
	public static final Dimension CONCEPT_ICON_SIZE = new Dimension(190,24);
	public static final Color CONCEPT_ICON_COLOR = new Color(100,255,100,50);
	
	// auto view event
    public static final String AUTO_VIEW_EVENT = "AutoViewChange";
    public static final String AUTO = "auto";
	
	// conditions for tasks
	public static final String CONDITION_USER_ACTION_COUNT = "User Action Count";
	public static final String CONDITION_USER_TYPE_COUNT = "User Type Count";
	public static final String CONDITION_CASE_START = "Case Start";
	public static final String CONDITION_CASE_FINISH = "Case Finish";
	public static final String CONDITION_CASE_TIME = "Case Time";
	public static final String CONDITION_TOTAL_TIME = "Total Time";
	public static final String CONDITION_CASE_COUNT = "Cases Count";
	public static final String CONDITION_EXPRESSION = "Expression";
	public static final String CONDITION_USER_ACTION = "User Action";
	public static final String CONDITION_TUTOR_RESPONSE = "Tutor Response";
	
	public static final String OPERATION_EQUALS = "=";
	public static final String OPERATION_GREATER_THEN = ">";
	public static final String OPERATION_LESS_THEN = "<";
	public static final String OPERATION_NOT_EQUALS = "!=";
	public static final String OPERATION_INTERVAL = "interval";
	public static final String OPERATION_AND = "and";
	public static final String OPERATION_OR = "or";
	public static final String OPERATION_NOT = "not";
	
	
	// pointer actions and parameters
	public static final String POINTER_ACTION_MOVE_VIEWER = "moveViewer";
	public static final String POINTER_ACTION_SHOW_ANNOTATION = "showAnnotation";
	public static final String POINTER_ACTION_CHANGE_IMAGE = "changeImage";
	public static final String POINTER_ACTION_COLOR_CONCEPT = "colorConcept";
	public static final String POINTER_ACTION_ADD_CONCEPT_ERROR = "addConceptError";
	public static final String POINTER_ACTION_ADD_CONCEPT = "addConceptEntry";
	public static final String POINTER_ACTION_REMOVE_CONCEPT = "removeConceptEntry";
	public static final String POINTER_ACTION_LOCK_INTERFACE_TO = "lockInterfaceTo";
	public static final String POINTER_ACTION_FLASH_CONCEPT = "flashConcept";
	public static final String POINTER_ACTION_FEEDBACK_MODE = "feedbackMode";
	public static final String POINTER_ACTION_FINISH_CASE = "finishCase";
	public static final String POINTER_ACTION_SWITCH_TUTOR_MODULE = "switchTutorModule";
	public static final String POINTER_ACTION_DO_LAYOUT = "doLayout";
	public static final String POINTER_ACTION_AUTO_HINT = "autoHint";
	public static final String POINTER_ACTION_PROMPT_USER = "promptUser";
	public static final String POINTER_ACTION_SET_INTERACTIVE = "setInteractive";
	public static final String POINTER_ACTION_SHOW_SKILLOMETER_QUESTION = "showSkillometerQuestion";
	public static final String POINTER_ACTION_SHOW_SKILLOMETER_DIALOG = "showSkillometerDialog";
	public static final String POINTER_ACTION_SOLVE_CASE_BUTTON = "solveCaseButton";
	public static final String POINTER_ACTION_ADD_MESSAGE_INPUT = "addMessageInput";
	
	public static final String POINTER_INPUT_LOCATION = "location";
	public static final String POINTER_INPUT_EXAMPLE = "example";
	public static final String POINTER_INPUT_RULER = "ruler";
	public static final String POINTER_INPUT_ALL = "all";
	public static final String POINTER_INPUT_SUFFIX_NOT_SEEN = "_not_seen";
	public static final String POINTER_INPUT_SUFFIX_NO_RULER = "_no_ruler";
	
	// student levels
	public static final String STUDENT_NOVICE = "novice";
	public static final String STUDENT_INTERMEDIATE = "intermediate";
	public static final String STUDENT_EXPERT = "expert";
	
	
	public static final String SCENARIO_TYPE_ERROR = "ERROR";
	public static final String SCENARIO_TYPE_HINT = "HINT";
	public static final String [] SCENARIO_TYPES = new String [] {"",TYPE_HINT,TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ATTRIBUTE,TYPE_ACTION,
		TYPE_HYPOTHESIS,TYPE_DIAGNOSIS,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK,TYPE_DONE };
	public static final String [] CONCEPT_TYPES = new String [] {TYPE_FINDING,TYPE_ABSENT_FINDING,TYPE_ATTRIBUTE,
		TYPE_HYPOTHESIS,TYPE_DIAGNOSIS,TYPE_SUPPORT_LINK,TYPE_REFUTE_LINK};
	
	// help tags in help messages
	public static final String TAG_CONCEPT = "<concept>";
	public static final String TAG_FINDING = "<finding>";
	public static final String TAG_ATTRIBUTE = "<attribute>";
	public static final String TAG_ATTRIBUTES = "<attributes>";
	public static final String TAG_POTENTIAL_ATTRIBUTES = "<potential_attributes>";
	public static final String TAG_DEFINING_ATTRIBUTES = "<defining_attributes>";
	public static final String TAG_POTENTIAL_DIAGNOSES = "<potential_diagnoses>";
	public static final String TAG_HYPOTHESIS = "<hypothesis>";
	public static final String TAG_DIAGNOSIS = "<diagnosis>";
	public static final String TAG_FEATURE = "<feature>";
	public static final String TAG_DEFINED_FEATURE = "<defined_feature>";
	public static final String TAG_POWER = "<power>";
	public static final String TAG_FINDINGS = "<findings>";
	public static final String TAG_CHILD = "<child>";
	public static final String TAG_CHILDREN = "<children>";
	public static final String TAG_PARENT = "<parent>";
	public static final String TAG_SIBLING = "<sibling>";
	public static final String TAG_TEMPLATE = "<template>";
	public static final String TAG_ACTION = "<action>";
	public static final String TAG_DEFINITION = "<definition>";
	public static final String TAG_VALUE = "<value>";
	public static final String TAG_ATTRIBUTE_VALUES = "<attribute_values>";
	public static final String TAG_TEXT = "<text>";
	public static final String TAG_CORRECT_VALUE = "<correct_value>";
	
	public static final String ANNOTATION_CROSS = "Cross";
	public static final String ANNOTATION_RULER = "Ruler";
	
	// question types
	public static final String QUESTION_TYPE_SOLVE = "solve";
	public static final String QUESTION_TYPE_COLOR = "color";
	public static final String QUESTION_TYPE_REPORT = "report";
	public static final String QUESTION_TYPE_LINK = "link";
	public static final String QUESTION_TYPE_INFER = "infer";
	public static final String QUESTION_TYPE_DIFFERENTIATE = "differentiate";
	public static final String QUESTION_TYPE_IDENTIFY= "identify";
	public static final String QUESTION_TYPE_FREE_TEXT= "free.text";
	public static final String QUESTION_TYPE_POINT_OUT= "point.out";
	public static final String QUESTION_TYPE_MULTIPLE_CHOICE = "multiple.choice";
	public static final String QUESTION_MODE_FOK = "fok";
	
	
	// some message input tags
	public static final String MESSAGE_INPUT_AUTO = "auto";
	public static final String MESSAGE_INPUT_CONFIDENCE = "confidence";
	public static final String MESSAGE_INPUT_LOCATION = "location";
	public static final String MESSAGE_INPUT_VIEW = "view";
	public static final String MESSAGE_INPUT_POWER = "power";
	public static final String MESSAGE_INPUT_FOK = "fok";
	public static final String MESSAGE_INPUT_RESOURCE_LINK = "resource.link";
	public static final String MESSAGE_INPUT_NUMERIC_VALUE = "numeric.value";
	public static final String MESSAGE_INPUT_WORKSHEET = "worksheet";
	
	// help scenarios/ error codes
	public static final String ERROR_OK = "OK";
	public static final String ERROR_REMOVED_CORRECT_CONCEPT = "Removed Correct Concept";
	public static final String ERROR_NO_SUCH_CONCEPT_IN_DOMAIN = "No Such Concept in Domain";
	
	//recommendation
	public static final String ERROR_ASSESMENT_IS_INCORRECT  = "Assesment is Incorrect";
	
	// attribute
	public static final String ERROR_ATTRIBUTE_IS_INCORRECT  = "Attribute is Incorrect";
	public static final String ERROR_INCORRECT_NEGATION_ATTRIBUTE  = "Attribute Negation is Incorrect ";
	public static final String ERROR_ATTRIBUTE_VALUE_IS_INCORRECT  = "Attribute Value is Incorrect";
	public static final String ERROR_ATTRIBUTE_NUMERIC_VALUE_IS_INCORRECT  = "Attribute Numeric Value is Incorrect";
	public static final String ERROR_ATTRIBUTE_NOT_VALID  = "Attribute Not Valid";
	public static final String ERROR_ATTRIBUTE_SET_NOT_VALID  = "Attribute Set Not Valid";
	public static final String ERROR_ATTRIBUTE_NOT_IMPORTANT = "Attribute is Not Important";
	public static final String ERROR_ATTRIBUTE_IN_INCORRECT_LOCATION = "Incorrect Attribute Location";
	
	// evidence
	public static final String ERROR_FINDING_NOT_IN_CASE = "Finding Not in Case";
	public static final String ERROR_FINDING_NOT_IN_CASE_BUT_SUPPORTS_DIAGNOSIS = "Finding Not in Case but Supports Diagnosis";
	public static final String ERROR_FINDING_IN_INCORRECT_LOCATION = "Incorrect Finding Location";
	public static final String ERROR_FINDING_DIFFERENT_AT_LOCATION = "Different Finding at Location";
	public static final String ERROR_FINDING_SHOULD_BE_ABSENT = "Finding Should be Absent";
	public static final String ERROR_FINDING_IS_TOO_GENERAL = "Finding is Too General";
	public static final String ERROR_FINDING_NOT_IMPORTANT = "Finding is Not Important";
	public static final String ERROR_FINDING_OBSERVED_AT_LOW_POWER = "Finding Observed at Low Power";
	public static final String ERROR_FINDING_IS_INCORRECT = "Finding is Incorrect";
	
	// absent evidence
	public static final String ERROR_ABSENT_FINDING_NOT_IN_CASE = "Absent Finding Not in Case";
	public static final String ERROR_ABSENT_FINDING_NOT_IMPORTANT = "Absent Finding is Not Important";
	public static final String ERROR_ABSENT_FINDING_IN_INCORRECT_LOCATION = "Incorrect Absent Finding Location";
	public static final String ERROR_ABSENT_FINDING_SHOULD_BE_PRESENT = "Absent Finding Should be Present";
	public static final String ERROR_ABSENT_FINDING_OBSERVED_AT_LOW_POWER = "Absent Finding Observed at Low Power";
	
	// hypothesis
	public static final String ERROR_HYPOTHESIS_DOESNT_MATCH_ANY_FINDINGS = "Hypothesis Doesn't Match Any Findings";
	public static final String ERROR_HYPOTHESIS_CONTRADICTS_SOME_FINDING = "Hypothesis Contradicts Some Findings";
	public static final String ERROR_HYPOTHESIS_BECAME_INCONSISTANT = "Hypothesis Became Inconsistent";
	public static final String ERROR_HYPOTHESIS_IS_TOO_GENERAL = "Hypothesis is Too General";
		
	// diagnosis
	public static final String ERROR_DIAGNOSIS_DOESNT_MATCH_ALL_FINDINGS = "Diagnosis Doesn't Match All Findings";
	public static final String ERROR_DIAGNOSIS_NO_FINDINGS = "Diagnosis while No Findings";
	public static final String ERROR_DIAGNOSIS_DOESNT_HAVE_ENOUGH_EVIDENCE = "Diagnosis Doesn't Have Enough Evidence";
	public static final String ERROR_DIAGNOSIS_BECAME_INCONSISTANT = "Diagnosis Became Inconsistent";
	public static final String ERROR_DIAGNOSIS_IS_CORRECT_BUT_NOT_SUPPORTED = "Diagnosis is Correct But Not Supported";
	public static final String ERROR_DIAGNOSIS_IS_SUPPORTED_BUT_NOT_GOAL = "Diagnosis is Supported But Not Goal";
	public static final String ERROR_DIAGNOSIS_IS_CORRECT = "Diagnosis is Correct";
	public static final String ERROR_DIAGNOSIS_IS_TOO_GENERAL = "Diagnosis is Too General";
	public static final String ERROR_DIAGNOSIS_IS_TOO_SPECIFIC = "Diagnosis is Too Specific";
	public static final String ERROR_DIAGNOSIS_NOT_SPECIFIC_ENOUGH = "Diagnosis Not Specific Enough";
	public static final String ERROR_DIAGNOSIS_IS_INCORRECT = "Diagnosis is Incorrect";
	
	
	// support link
	public static final String ERROR_LINK_FINDING_DOES_NOT_SUPPORT_HYPOTHESIS = "Finding Doesn't Support Hypothesis";
	public static final String ERROR_LINK_FINDING_PARENT_SUPPORTS_HYPOTHESIS = "Finding's Parent Supports Hypothesis";
	public static final String ERROR_LINK_FINDING_NO_LONGER_SUPPORTS_HYPOTHESIS = "Finding No Longer Supports Hypothesis";
	
	// refute link
	public static final String ERROR_LINK_FINDING_DOES_NOT_REFUTE_HYPOTHESIS = "Finding Doesn't Refute Hypothesis";
	public static final String ERROR_LINK_NO_EVIDENCE_TO_REFUTE_HYPOTHESIS = "No Evidence that Finding Refutes Hypothesis";
	public static final String ERROR_LINK_FINDING_NO_LONGER_REFUTES_HYPOTHESIS = "Finding No Longer Refutes Hypothesis";
	
	// actions
	public static final String ERROR_ACTION_INCOMPLETE = "Action Incomplete";
	public static final String ERROR_MEASURE_RULER_INCORRECT_LOCATION = "Measure Ruler Incorrect Location";
	public static final String ERROR_MEASURE_RULER_INCORRECT_SIZE = "Measure Ruler Incorrect Size";
	public static final String ERROR_MEASURE_RULER_INCORRECT_ANGLE = "Measure Ruler Incorrect Angle";
	
	
	// done
	public static final String ERROR_CASE_NOT_SOLVED = "Case Not Solved";
	public static final String ERROR_CASE_NEEDS_DIAGNOSIS_TO_FINISH = "Case Needs Diagnosis to Finish";
	public static final String ERROR_CASE_HAS_MORE_DIAGNOSES_TO_CONSIDER = "Case Has More Diagnoses to Consider";
	public static final String ERROR_CASE_HAS_MORE_FINDINGS_TO_CONSIDER = "Case Has More Findings to Consider";
	public static final String ERROR_CASE_NEEDS_REFINED_FINDINGS = "Case Needs Refined Findings";
	
	// happy action states
	public static final String HINT_MEASURE_RULER_DONE = "Measure Ruler Done";
	public static final String HINT_MEASURE_HPF_DONE = "Measure HPF Done";
	public static final String HINT_MEASURE_MM2_DONE = "Measure MM2 Done";
	public static final String HINT_OBSERVE_ALL_DONE = "Observe All Done";
	
	// hint scenarios
	public static final String HINT_NOTHING_DONE_YET = "Nothing is Done Yet";
	public static final String HINT_MISSING_HEADER = "Missing Header";
	public static final String HINT_MISSING_WORKSHEET = "Missing Worksheet";
	public static final String HINT_MISSING_FINDING = "Missing Finding";
	public static final String HINT_MISSING_SIMILAR_FINDING = "Missing Similar Finding";
	public static final String HINT_MISSING_SIMILAR_ABSENT_FINDING = "Missing Similar Absent Finding";
	public static final String HINT_MISSING_ATTRIBUTE = "Missing Attribute";
	public static final String HINT_MISSING_MODIFIER = "Missing Modifier";
	public static final String HINT_MISSING_ACTION = "Missing Action";
	public static final String HINT_MISSING_VALUE = "Missing Value";
	public static final String HINT_MISSING_NEGATION = "Missing Negation";
	public static final String HINT_MISSING_ANOTHER_ATTRIBUTE = "Missing Another Attribute";
	public static final String HINT_MISSING_DEFINING_ATTRIBUTE = "Missing Defining Attribute";
	public static final String HINT_MISSING_ABSENT_FINDING = "Missing Absent Finding";
	public static final String HINT_MISSING_HYPOTHESIS = "Missing Hypothesis";
	public static final String HINT_MISSING_DIAGNOSIS = "Missing Diagnosis";
	public static final String HINT_NO_FINDINGS_YET  = "No Findings Yet";
	public static final String HINT_NO_HYPOTHESES_YET = "No Hypotheses Yet";
	public static final String HINT_NO_DIAGNOSES_YET = "No Diagnoses Yet";
	public static final String HINT_CASE_SOLVED = "Case Solved";
	public static final String HINT_MISSING_RULER_ACTION = "Missing Ruler Action";
	public static final String HINT_MISSING_HPF_ACTION = "Missing HPF Action";
	public static final String HINT_MISSING_MM2_ACTION = "Missing MM2 Action";
	public static final String HINT_MISSING_OBSERVE_ALL_ACTION = "Missing Observe All Action";
	
	// property fields
	public static final String PROPERTY_CASE_DONE = "Case Solved";
	public static final String PROPERTY_NODE_SELECTED = "Node Selected";
	public static final String PROPERTY_NODE_DESELECTED = "Node Deselected";
	public static final String PROPERTY_NODE_MOVED = "Node Moved";
	
	// list of errors that are irrelevent
	public static final String [] IRRELEVANT_ERRORS = new String [] {ERROR_FINDING_NOT_IMPORTANT, ERROR_ABSENT_FINDING_NOT_IMPORTANT, 
																	 ERROR_ATTRIBUTE_NOT_IMPORTANT,ERROR_ABSENT_FINDING_NOT_IN_CASE};
	
}
