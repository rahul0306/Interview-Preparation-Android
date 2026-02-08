package com.example.interviewprep.navigation

sealed class InterviewPrepScreens(val route: String) {

    data object Login : InterviewPrepScreens("login")
    data object Signup : InterviewPrepScreens("signup")
    data object Upload: InterviewPrepScreens("upload")
    data object Interview: InterviewPrepScreens("interview")
    data object Review: InterviewPrepScreens("review")

    data object Recordings : InterviewPrepScreens("recordings")

}