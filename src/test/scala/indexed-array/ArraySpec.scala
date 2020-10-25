package sportarray

import org.scalatest._
import org.scalatest.matchers.should.Matchers
import org.scalatest.featurespec.{AnyFeatureSpec}
import com.github.nscala_time.time.Imports._

import sportdate.SportDate
import sportdate.{IsSportDateInstances, IsSportDateSyntax}

import Skeleton.{IsBase, IsElement}
import IndicesObj._
import shapeless._
//import shapeless.test.{illTyped}
import shapeless.ops.hlist._

import ListOfListsObj._

object Dummy {
  val values1d = List(0.1, 0.2, 0.3, 0.4, 0.5)
  val values2d = List(values1d, values1d.map(_ + 1), values1d.map(_ + 2))
  val values3d = List(
    List(
      List(0.1, 0.2, 0.3, 0.4, 0.5),
      List(1.1, 1.2, 1.3, 1.4, 1.5),
      List(2.1, 2.2, 2.3, 2.4, 2.5),
    ),
    List(
      List(3.1, 3.2, 3.3, 3.4, 3.5),
      List(4.1, 4.2, 4.3, 4.4, 4.5),
      List(5.1, 5.2, 5.3, 5.4, 5.5),
    ),
  )
  val list1d = List1d[Double](values1d)
  val list2d = List2d[Double](values2d)
  val list3d = List3d[Double](values3d)
}
  

class ArraySpec extends AnyFeatureSpec with GivenWhenThen with Matchers {
  import ArrayDefs._

  feature("Arraylike objects should be able to implement IsArray") {
    case class A1[T](data: List[T])
    implicit def a1ev[T: IsElement] = IsArray[A1[T], T](
      self => A1[T](Nil: List[T]),
      (self, n) => self.data(n),
      self => self.data.length,
      (self, o) => A1[T](o :: self.data),
    )

    scenario("A 1d type that can implement IsArray, implements IsArray") {
      Given("A 1-d arraylike type and an implicit conversion to IsArray")
      When("A valid IsElement is used")
      Then("It should compile")
      "implicitly[IsArray[A1[Double]]]" should compile

      When("A bad IsElement is used")
      case class BadElem()
      Then("It should not compile")
      "implicitly[IsArray[A1[BadElem]]]" shouldNot typeCheck
    }

    scenario("A 2d 1dOf1d type that can implement IsArray, implements IsArray") {
      Given("A 1dOf1d arraylike type and an implicit conversion to IsArray")
      case class A1OfA1[T](data: List[A1[T]])
      implicit def a1ofa1ev[T: IsElement] = IsArray[A1OfA1[T], A1[T]](
        self => A1OfA1[T](List(A1[T](Nil: List[T]))),
        (self, n) => self.data(n),
        self => self.data.length,
        (self, o) => A1OfA1[T](o :: self.data),
      )
      When("A valid IsElement is used")
      Then("It should compile")
      "implicitly[IsArray[A1OfA1[Double]]]" should compile

      When("A bad IsElement is used")
      Then("It should not compile")
      "implicitly[IsArray[A1OfA1[BadElem]]]" shouldNot typeCheck
    }

    scenario("A 2d list-of-list type that can implement IsArray, implements IsArray") {
      Given("A 2d list-of-list type and an implicit conversion to IsArray")
      case class A2[T](data: List[List[T]])
      implicit def a2ev[T: IsElement] = IsArray[A2[T], A1[T]](
        self => A2[T](List(List())),
        (self, n) => A1[T](self.data(n)),
        self => self.data.length,
        (self, o) => A2[T](a1ev.toList(o) :: self.data),
      )
      When("A valid IsElement is used")
      Then("It should compile")
      "implicitly[IsArray[A2[Double]]]" should compile
     
      When("A bad IsElement is used")
      Then("It should not compile")
      "implicitly[IsArray[A2[BadElem]]]" shouldNot typeCheck
    }
  }

  feature("Implicit class conversions and typeclass syntax for IsArray implementations") {
    scenario("The user tries to access IsArray methods from the implementing type") {
      Given("An arraylike value and an implicit conversion to IsArray")
      case class A1[T](data: List[T])
      implicit def a1ev[T: IsElement] = IsArray[A1[T], T](
        self => A1[T](Nil: List[T]),
        (self, n) => self.data(n),
        self => self.data.length,
        (self, o) => A1[T](o :: self.data),
      )
      val t1 = A1[Double](List(1, 2, 3))
      When("Implicit conversion is in scope")
      import IsArraySyntax._
      Then("IsArray syntax should be available")
      val c = implicitly[A1[Double] => IsArrayOps[A1[Double], Double]]
      assert(t1.data.zipWithIndex.forall(t => t1.getAtN(t._2) == t._1))
    }
  }

  info("IsXd typeclasses are a way to set the dimensionality of a given array")
  feature("IsXd typeclass") {
    scenario("An Is2d arraylike returns a value") {
      Given("An 2-d arraylike which returns a 1-d arraylike")
      case class A1[T](data: List[T])
      implicit def a1IsArray[T: IsElement] = IsArray[A1[T], T](
        self => A1[T](Nil: List[T]),
        (self, n) => self.data(n),
        self => self.data.length,
        (self, o) => A1[T](o :: self.data),
      )
      //implicit def a1Is1d[T: Element] = Is1d[A1[T], T]
      case class A1OfA1[T](data: List[A1[T]])
      implicit def a1ofa1ev[T: IsElement] = IsArray[A1OfA1[T], A1[T]](
        self => A1OfA1[T](List(A1[T](Nil: List[T]))),
        (self, n) => self.data(n),
        self => self.data.length,
        (self, o) => A1OfA1[T](o :: self.data),
      )
      When("Implementation of Is2d is attempted without Is1d implemented for the 1-d array")
      Then("It should fail to compile")
      "implicit def a1Of1Is2d[T: IsElement] = Is2d[A1OfA1[T], A1[T]]" shouldNot typeCheck
      
      When("Implementation of Is2d is attempted with Is1d implemented for the 1-d array")
      Then("It should compile")
      implicit def a1Is1d[T: IsElement] = Is1d[A1[T], T] 
      "implicit def a1Of1Is2d[T: IsElement] = Is2d[A1OfA1[T], A1[T]]" should compile
    }
  }

  feature("Multi-dimensional arrays") {
    case class A1[T](data: List[T])
    implicit def a1IsArray[T: IsElement] = IsArray[A1[T], T](
      self => A1[T](Nil: List[T]),
      (self, n) => self.data(n),
      self => self.data.length,
      (self, o) => A1[T](o :: self.data),
    )
    case class A1OfA1[T](data: List[A1[T]])
    implicit def a1ofa1IsArray[T: IsElement] = IsArray[A1OfA1[T], A1[T]](
      self => A1OfA1[T](List(A1[T](Nil: List[T]))),
      (self, n) => self.data(n),
      self => self.data.length,
      (self, o) => A1OfA1[T](o :: self.data),
    )
    implicit def a1Is1d[T: IsElement] = Is1d[A1[T], T] 
    implicit def a1Of1Is2d[T: IsElement] = Is2d[A1OfA1[T], A1[T]]
    scenario("A value is returned from a 2d-dimensional array using getAtN") {
      Given("A 1-dimensional arraylike, and a 2d list of 1d arraylike 2d array implementation")
      When("getAtN is called on a concrete instance of the 2d arraylike")
      import ArrayDefs.IsArraySyntax._
      val t2 = A1OfA1[Double](A1[Double](List(1.0, 2.0)) :: Nil)
      Then("the returned value should be the 1d arraylike")
      val t1: A1[Double] = t2.getAtN(0)
    }
    scenario("A value is returned from a 3d-dimensional array using getAtN") {
      Given("A 3d arraylike")
      val t3 = List3d[Double](Dummy.values3d)
      When("getAtN is called on a concrete instance of the 3d arraylike")
      import ArrayDefs.IsArraySyntax._
      Then("the returned value should be the 1d arraylike")
      val t2: List2d[Double] = t3.getAtN(0)
    }
  }

  feature("IsArray methods") {
    import Dummy._
    scenario("getAtN is called on a 1d Array to recover its original elements") {
      Given("A 1d arraylike")
      import ArrayDefs.IsArraySyntax._
      val list1d = List1d[Double](values1d)
      When("getAtN is called for its elements")
      Then("It should return the originally input values")
      assert(values1d.zipWithIndex.forall({case(x, i) => x == list1d.getAtN(i)}))
    }
    scenario("IsArray objects are constructed from individual values using cons") {
      Given("A list of values implementing IsElement")
      import ArrayDefs.IsArraySyntax._
      val lst0 = List1d[Double](Nil)
      When("A 1d arraylike is constructed by cons-ing these values")
      val lst1 = values1d(4) :: lst0
      val lst2 = values1d(3) :: lst1
      val lst3 = values1d(2) :: lst2
      val lst4 = values1d(1) :: lst3
      val lst5 = values1d(0) :: lst4
      Then("It should reconcile with the original values")
      assert(lst5 == List1d[Double](values1d))
    }
  }

  feature("Using the getILoc method to reduce down an IsArray using integer references") {
    import Dummy._
    val list1d = List1d[Double](values1d)
    val list2d = List2d[Double](values2d)
    def checkGetILocWithInt[A, _E: IsBase](
      a: A, f:(A, Int) => A,
    ) (implicit aIsArr: IsArray[A] { type E = _E },
    ): Boolean = {
      import ArrayDefs.IsArraySyntax._
      (0 to a.length - 1).forall(n => f(a, n) == a.getAtN(n) :: a.getEmpty)
    }
    def checkGetILocWithListInt[A, _E: IsBase](
      a: A, f:(A, List[Int]) => A,
    ) (implicit aIsArr: IsArray[A] { type E = _E },
    ): Boolean = {
      import ArrayDefs.IsArraySyntax._
      (0 to a.length - 2).forall(n => f(a, List(n, n + 1)) == { 
        println(f(a, List(n, n + 1)))
        println(a.getAtN(n) :: a.getAtN(n + 1) :: a.getEmpty)
        a.getAtN(n) :: a.getAtN(n + 1) :: a.getEmpty
      })
    }
    scenario("getILoc is called with null to return the entire array") {
      Given("A 1-dimensional arraylike")
      When("getILoc is called with a null argument")
      import ArrayDefs.IsArraySyntax._
      val t1 = list1d.getILoc(null)
      Then("the entire array should be returned")
      assert(t1 == list1d)
      
      Given("A 2-dimensional arraylike")
      When("getILoc is called with a null argument")
      import ArrayDefs.IsArraySyntax._
      val t2 = list2d.getILoc(null)
      Then("the entire array should be returned")
      assert(t2 == list2d)
    }
    scenario("getILoc is called with an Int to return the appropriate element") {
      import ArrayDefs.IsArraySyntax._
      When("getILoc is called with an Int on a 1d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the reference")
      assert(checkGetILocWithInt[List1d[Double], Double](list1d, (l, i) => l.getILoc(i)))

      When("getILoc is called with an Int on a 2d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the reference")
      assert(checkGetILocWithInt[List2d[Double], List1d[Double]](list2d, (l, i) => l.getILoc(i)))
    }
    scenario("getILoc is called with a List of Ints to return the appropriate elements") {
      import ArrayDefs.IsArraySyntax._
      When("getILoc is called with an List of Ints on a 1d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the references")
      assert(checkGetILocWithListInt[List1d[Double], Double](list1d, (l, i) => l.getILoc(i)))

      When("getILoc is called with an List of Ints on a 2d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the references")
      assert(checkGetILocWithListInt[List2d[Double], List1d[Double]](list2d, (l, i) => l.getILoc(i)))
    }
    scenario("getILoc is called with an HList of Ints to return the appropriate elements") {
      import ArrayDefs.IsArraySyntax._
      When("getILoc is called with an HList of Ints on a 1d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the reference")
      assert(
        checkGetILocWithInt[List1d[Double], Double](list1d, (l, i) => l.getILoc(i :: HNil))
      )
      When("getILoc is called with an HList of Ints on a 2d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the reference")
      assert(
        checkGetILocWithInt[List2d[Double], List1d[Double]](list2d, (l, i) => l.getILoc(i :: HNil))
      )
    }
    scenario("getILoc is called with an HList of List[Int] to return the appropriate elements") {
      import ArrayDefs.IsArraySyntax._
      When("getILoc is called with an HList of List[Int] on a 1d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the references")
      assert(
        checkGetILocWithListInt[List1d[Double], Double](list1d, (l, i) => l.getILoc(i :: HNil))
      )
      When("getILoc is called with an HList of List[Int] on a 2d arraylike")
      Then("it returns a same-dimensional array with the top dimension reduced down to the references")
      assert(
        checkGetILocWithListInt[List2d[Double], List1d[Double]](list2d, (l, i) => l.getILoc(i :: HNil))
      )
    }
  }

  feature("The IsArray.length method returns the length of the top dimension") {
    import Dummy._
    import ArrayDefs.IsArraySyntax._
    scenario("An xd array of y elements should return .length of y")
    {
      When(".length is run on list1d")
      Then("It should return the correct length")
      assert(list1d.length == list1d.data.length)
      When(".length is run on list2d")
      Then("It should return the correct length")
      assert(list2d.length == list2d.data.length)
      When(".length is run on list3d")
      Then("It should return the correct length")
      assert(list3d.length == list3d.data.length)
    }
  }

  feature("An Updatable array can be updated with .setElem") {
    scenario("Using setElem with a new valid value creates a new array of the same type") {
      import Dummy._
      import ArrayDefs.IsUpdatable
      When(".setElem with a 1d Array")
      implicit def list1dIsUpdatable[T: IsElement] = IsUpdatable[List1d[T], T]
      val t1 = list1d.setElem(1, 2.2) 
      assert(t1.data(1) == 2.2)
    }
    scenario("Using setElem with an invalid value does not compile") {
    }
  }

  feature("An Updatable array can be updated with .setILoc") {
    scenario("A 1d array returns a same-size 1d array if .setILoc is used") {
    }
  }
}


  //"Arr1d" should "have shape(0) == length" in {
    //import ArrayDefs.IsSpArrSyntax._
    //val list1d = List1d[Dim2T, Double](dim2, values1d)
    //assert(list1d.length == values1d.length)
    //assert(list1d.length == dim2.length)
  //}
  //"Arr1d" should "use fMap to apply functions to its elements" in {
    //import ArrayDefs.IsSpArrSyntax._
    //val list1d = List1d[Dim2T, Double](dim2, values1d)
    //val listmapped = list1d.fmap(_: Double => 'a')
    //val c = list1d.fmap(_: String => 'a')
    //assert(listmapped.toList.forall(_ == 'a'))
  //}
  ////"Arr1d" should "return correct Datum with .loc" in {
    ////import ArrayDefs._
    ////import ArrayDefs.IsSpArrSyntax._
    ////val list1d = List1d[PositionsData, Dim2T](dim2, values1d)
    ////assert(
      ////dim2.toList.zip(values1d).forall({case(d, v) => list1d.loc(d) == Some(v)})
    ////)
  ////}
  ////"Arr2d" should "return a 1d array with .iloc using Int on the second dimension" in {
    ////import ArrayDefs._
    ////import ArrayDefs.Is2dSpArrSyntax._
    ////val list2d = List2d[Dim1T, Dim2T, PositionsData]((dim1, dim2), values2d)
    ////assert(
      ////values2d.zipWithIndex.forall({case(x, i) => 
        ////list2d.iloc(i) == List1d[Dim2T, PositionsData](dim2, x)
      ////})
    ////)
  ////}
  ////"Arr2d" should "return a correct Arr1d with .loc" in {
    ////val arr2d = Arr2d[Dim1T, Dim2T, PositionsData]((dim1, dim2), values2d)
    ////assert(
      ////arr2d.loc(dim1.vals(2)) == Some(Arr1d[Dim2T, PositionsData](dim2, values2d(2)))
    ////)
    ////assert(
      ////arr2d.loc(dim2.vals(1)) == Some(Arr1d[Dim1T, PositionsData](dim1, values2d.map(_(1))))
    ////)
  ////}
  ////"Arr3d" should "return a correct Arr2d with .loc" in {
    ////val arr3d = Arr3d[Dim0T, Dim1T, Dim2T, PositionsData]((dim0, dim1, dim2), values3d)
    ////val dim0sliceat0 = values3d(0)
    ////assert(
      ////arr3d.loc(dim0.vals(0)) == Some(Arr2d[Dim1T, Dim2T, PositionsData]((dim1, dim2), dim0sliceat0))
    ////)
    ////val dim1sliceat1 = List(
      ////List(1.1, 1.2, 1.3, 1.4, 1.5),
      ////List(4.1, 4.2, 4.3, 4.4, 4.5),
    ////)
    ////assert(
      ////arr3d.loc(dim1.vals(1)) == Some(Arr2d[Dim0T, Dim2T, PositionsData]((dim0, dim2), dim1sliceat1))
    ////)
    ////val dim2sliceat2 = List(
      ////List(0.3, 1.3, 2.3),
      ////List(3.3, 4.3, 5.3),
    ////)
    ////assert(
      ////arr3d.loc(dim2.vals(2)) == Some(Arr2d[Dim0T, Dim1T, PositionsData]((dim0, dim1), dim2sliceat2))
    ////)
  ////}
//}
