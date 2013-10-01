package com.roundeights.shnappy.admin

import org.specs2.mutable._
import org.specs2.mock._

import com.roundeights.shnappy.Env
import com.roundeights.hasher.Algo
import java.util.UUID

class SessionTest extends Specification with Mockito {

    val env = mock[Env]
    env.secretKey returns Algo.sha1("secrect")

    // A shared session builder
    val session = new Session( env, () => 50000000L )

    // A sample user ID
    val userID = UUID.fromString("8880fccd-82c3-48e7-8caf-587db71585e6")

    // Sample user
    val user = mock[User]
    user.id returns userID


    "A session object" should {

        "Consistently hash for a given time period" in {
            session.hmac("key", 0).hex must_== (
                "bc9f5ad1c82edd4ea7550cd8e914d3806120f5ace17af236bb" +
                "71fc548c3a56d8674343e8025d8c0f26109f984bed0969bb2f" +
                "e0c1f1c2c0631d77eaabe6890c56"
            )

            session.hmac("key", 1).hex must_== (
                "edbf63b994d04ba5cab64a0917a2fde827f971bd4ee6964a42" +
                "c40989c2bab817738a51483470a96aaf4873f2e503d91f3d50" +
                "234add9797805c0ecccb7ffeda67"
            )
        }

        "Hash a user and email address together to create a token" in {
            session.token("a@b.com", user) must_== (
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "4ee8e00de1602d1b5ad832ff2fa1c0233f807c0e01a7" +
                "2576624bac5c73749d5e2cff58c6cb042b7ba741b54b" +
                "ba23fc823b0ef9e8aaa0a328f704e6415be7bc92"
            )
        }

        "Validate recently generated tokens" in {
            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "4ee8e00de1602d1b5ad832ff2fa1c0233f807c0e01a7" +
                "2576624bac5c73749d5e2cff58c6cb042b7ba741b54b" +
                "ba23fc823b0ef9e8aaa0a328f704e6415be7bc92"
            ) must_== Some("a@b.com" -> userID)

            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "dd3baa125138a1873148003eed31313c0f3de8cc2f47" +
                "461bc48a04e577c7509a84bc613b6c5ea22e769b74a7" +
                "b0242871fea76dbe033aaa4d78d9e113e18c52f8"
            ) must_== Some("a@b.com" -> userID)
        }

        "Reject tokens that are too old" in {
            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "bb7edadec8980d4a080cdfde62f3f8f7881b05870704" +
                "8b4f71b26b8ae00a37d2de468ea1ddd5588e464af3b6" +
                "2d599f5a140d64eecd26f703b6269fc21f366c2f"
            ) must_== None
        }

        "Reject tokens where the hash doesn't match" in {
            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa" +
                "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb" +
                "cccccccccccccccccccccccccccccccccccccccc"
            ) must_== None
        }

        "Reject tokens that are too short" in {
            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "dd3baa125138a1873148003eed31313c0f3de8cc2f47"
            ) must_== None

            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6"
            ) must_== None

            session.checkToken("a@b.com|8880fccd") must_== None
        }

        "Trim tokens that are too long" in {
            session.checkToken(
                "a@b.com|8880fccd-82c3-48e7-8caf-587db71585e6" +
                "4ee8e00de1602d1b5ad832ff2fa1c0233f807c0e01a7" +
                "2576624bac5c73749d5e2cff58c6cb042b7ba741b54b" +
                "ba23fc823b0ef9e8aaa0a328f704e6415be7bc92XXXX"
            ) must_== Some("a@b.com" -> userID)
        }

        "Reject tokens without an email" in {
            session.checkToken(
                "8880fccd-82c3-48e7-8caf-587db71585e6" +
                "cf7abb15375a4e6051eeef68b1cfd56ff1b9" +
                "c27d734856444ddee2a6f001ac09b5b32eed" +
                "4842a05cbd17208e15450c22f2644a042ee6" +
                "fbc9f977e655b09c76cc"
            ) must_== None
        }

    }
}

