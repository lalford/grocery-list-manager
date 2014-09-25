package controllers

import org.scalatestplus.play._

class RequestHelpersTest extends PlaySpec {

  "evaluate expression" must {
    "work with an integer" in {
      val expected = Option(1.toDouble)
      RequestHelpers.evaluate("1") mustBe expected
    }

    "work with a decimal" in {
      val expected = Option(2.345)
      RequestHelpers.evaluate("2.345") mustBe expected
    }

    "work with a simple fraction" in {
      val expected = Option(1.2)
      RequestHelpers.evaluate("6/5") mustBe expected
    }

    "work with an arithmetic expression ignoring whitespace" in {
      val expected = Option(1.2)
      RequestHelpers.evaluate("1+1   /      5") mustBe expected
    }

    // it doesn't, only arithmetic
//    "work with an expression of quantity" in {
//      val expected = Option(1.5)
//      RequestHelpers.evaluate("1 1/2") mustBe expected
//    }
  }

}