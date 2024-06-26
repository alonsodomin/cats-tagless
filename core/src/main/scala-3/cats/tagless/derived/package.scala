/*
 * Copyright 2019 cats-tagless maintainers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cats.tagless.derived

import cats.*
import cats.arrow.Profunctor
import cats.tagless.Derive

import scala.annotation.experimental

extension (x: Functor.type) @experimental inline def derived[F[_]]: Functor[F] = Derive.functor
extension (x: Contravariant.type) @experimental inline def derived[F[_]]: Contravariant[F] = Derive.contravariant
extension (x: Invariant.type) @experimental inline def derived[F[_]]: Invariant[F] = Derive.invariant
extension (x: Semigroupal.type) @experimental inline def derived[F[_]]: Semigroupal[F] = Derive.semigroupal
extension (x: Apply.type) @experimental inline def derived[F[_]]: Apply[F] = Derive.apply
extension (x: FlatMap.type) @experimental inline def derived[F[_]]: FlatMap[F] = Derive.flatMap
extension (x: SemigroupK.type) @experimental inline def derived[F[_]]: SemigroupK[F] = Derive.semigroupK
extension (x: MonoidK.type) @experimental inline def derived[F[_]]: MonoidK[F] = Derive.monoidK
extension (x: Bifunctor.type) @experimental inline def derived[F[_, _]]: Bifunctor[F] = Derive.bifunctor
extension (x: Profunctor.type) @experimental inline def derived[F[_, _]]: Profunctor[F] = Derive.profunctor
