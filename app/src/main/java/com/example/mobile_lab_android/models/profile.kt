package com.example.mobile_lab_android.models

data class ProfileModel(
    var name: String = "-",
    var email: String = "-",
    var dateOfBirth: String = "-",
    var phoneNumber: String = "-",
    var address: String = "-",
    var bio: String = "-",
    var occupation: String = "-",
    var website: String = "-",
    var socialMedia: String = "-",
    var additionalInfo: String = "-"
) {
    companion object {
        fun fromMap(data: Map<String, Any?>): ProfileModel {
            return ProfileModel(
                name = data["name"] as? String ?: "-",
                email = data["email"] as? String ?: "-",
                dateOfBirth = data["dateOfBirth"] as? String ?: "-",
                phoneNumber = data["phoneNumber"] as? String ?: "-",
                address = data["address"] as? String ?: "-",
                bio = data["bio"] as? String ?: "-",
                occupation = data["occupation"] as? String ?: "-",
                website = data["website"] as? String ?: "-",
                socialMedia = data["socialMedia"] as? String ?: "-",
                additionalInfo = data["additionalInfo"] as? String ?: "-"
            )
        }
    }

    fun toMap(): Map<String, Any> {
        return mapOf(
            "name" to name,
            "email" to email,
            "dateOfBirth" to dateOfBirth,
            "phoneNumber" to phoneNumber,
            "address" to address,
            "bio" to bio,
            "occupation" to occupation,
            "website" to website,
            "socialMedia" to socialMedia,
            "additionalInfo" to additionalInfo
        )
    }
}