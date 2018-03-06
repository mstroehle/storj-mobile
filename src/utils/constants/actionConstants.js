const ACTIONS = {
    //register constants
    REGISTER_SUCCESS: 'REGISTER_SUCCESS',
    REGISTER_ERROR: 'REGISTER_ERROR',
    REGISTER: 'REGISTER',

    //login constans
    LOGIN_SUCCESS: "LOGIN_SUCCESS",
    LOGIN_ERROR: "LOGIN_ERROR",
    LOGIN: "LOGIN"
};

//main reducer action constants
export const MAIN_ACTIONS = {
    SHOW_ACTION_BAR: 'SHOW_ACTION_BAR',
    HIDE_ACTION_BAR: 'HIDE_ACTION_BAR',
    SELECT_BUCKET: 'SELECT_BUCKET',
    DESELECT_BUCKET: 'DESELECT_BUCKET',
    GET_BUCKETS: 'GET_BUCKETS',
    CREATE_BUCKET: 'CREATE_BUCKET',
    DELETE_BUCKET: 'DELETE_BUCKET',
    ENABLE_SELECTION_MODE: 'ENABLE_SELECTION_MODE',
    DISABLE_SELECTION_MODE: 'DISABLE_SELECTION_MODE',
    SINGLE_ITEM_ACTIONS_SELECTED: 'SINGLE_ITEM_ACTIONS_SELECTED',
    SHOW_CREATE_BUCKET_INPUT: 'SHOW_CREATE_BUCKET_INPUT',
    HIDE_CREATE_BUCKET_INPUT: 'HIDE_CREATE_BUCKET_INPUT',
    SET_FIRST_SIGN_IN: 'SET_FIRST_SIGN_IN',
    REMOVE_FIRST_SIGN_IN: 'REMOVE_FIRST_SIGN_IN',
    SET_LOADING: 'SET_LOADING',
    UNSET_LOADING: 'UNSET_LOADING',
    OPEN_BUCKET: "OPEN_BUCKET",
    CLOSE_BUCKET: "CLOSE_BUCKET",
    SET_GRID_VIEW: "SET_GRID_VIEW",
    SET_LIST_VIEW: "SET_LIST_VIEW",
    CLEAR_SELECTION: "CLEAR_SELECTION",
    SET_SELECTION_ID: "SET_SELECTION_ID"
};

export const FILE_ACTIONS = {
    LIST_FILES: "LIST_FILES",
    UPLOAD_FILE_START: "UPLOAD_FILE_START",
    UPLOAD_FILE_SUCCESS: "UPLOAD_FILE_SUCCESS",
    UPLOAD_FILE_ERROR: "DOWNLOAD_FILE_ERROR",
    DOWNLOAD_FILE_SUCCESS: "DOWNLOAD_FILE_SUCCESS",
    DOWNLOAD_FILE_ERROR: "DOWNLOAD_FILE_ERROR", 
    DELETE_FILE: "DELETE_FILE",
    SELECT_FILE: "SELECT_FILE",
    DESELECT_FILE: "DESELECT_FILE",
    UPDATE_FILE_UPLOAD_PROGRESS: "UPDATE_FILE_UPLOAD_PROGRESS",
    UPDATE_FILE_DOWNLOAD_PROGRESS: "UPDATE_FILE_DOWNLOAD_PROGRESS",
    FILE_DOWNLOAD_CANCELED: "FILE_DOWNLOAD_CANCELED",
    FILE_UPLOAD_CANCELED: "FILE_UPLOAD_CANCELED"
};

export const FAVOURITES_ACTIONS = {
    SET_FAVOURITE: "SET_FAVOURITE",
    REMOVE_FAVOURITE: "REMOVE_FAVOURITE"
}

export default ACTIONS;