package io.getstream.chat.android.groupedchannels

/**
 * A user that can be selected on the login screen.
 */
data class LoginUser(
    val id: String,
    val name: String,
    val token: String,
) {
    companion object {
        val member01 = LoginUser(
            id = "member_01",
            name = "member_01",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWVtYmVyXzAxIn0.JEXL5-mvLcz96EG-CUSbdYgY-hex3iqktL75uSi_Uoo",
        )

        val member02 = LoginUser(
            id = "member_02",
            name = "member_02",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWVtYmVyXzAyIn0.z9gG7F9u-td_It3WA2kGOkI_Li5TtrcFh3YAi4AxgT0",
        )

        val member03 = LoginUser(
            id = "member_03",
            name = "member_03",
            token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyX2lkIjoibWVtYmVyXzAzIn0.G5e_HucwuVmWKB6NjuE-izAltTxH_k-AyY5RlAo-2VY",
        )

        val all: List<LoginUser> = listOf(member01, member02, member03)
    }
}
