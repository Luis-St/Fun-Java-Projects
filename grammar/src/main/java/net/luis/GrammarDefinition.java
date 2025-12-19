/*
 * Fun-Java-Projects
 * Copyright (C) 2025 Luis Staudt
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.luis;

import net.luis.utils.io.token.actions.TokenActions;
import net.luis.utils.io.token.actions.core.GroupingMode;
import net.luis.utils.io.token.grammar.GrammarBuilder;
import net.luis.utils.io.token.rules.TokenRule;
import net.luis.utils.io.token.rules.TokenRules;
import net.luis.utils.io.token.tokens.Token;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author Luis-St
 *
 */

public record GrammarDefinition(@NotNull GrammarBuilder builder) {
	
	public void defineRules() {
		this.builder.defineRule("Identifier", TokenRules.pattern("[a-zA-Z_][a-zA-Z0-9_]*"));
		this.builder.defineRule("FullQualifiedName", TokenRules.sequence(
			TokenRules.reference("Identifier"),
			TokenRules.zeroOrMore(
				TokenRules.sequence(
					TokenRules.value('.', false),
					TokenRules.reference("Identifier")
				)
			)
		));
	}
	
	public void addPreRules() {
		//region Character and String Literals
		this.builder.addRule(TokenRules.boundary(
			TokenRules.value('"', false).exactly(3),
			TokenRules.alwaysMatch(),
			TokenRules.value('"', false).exactly(3)
		), TokenActions.grouping(GroupingMode.MATCHED));
		
		this.builder.addRule(TokenRules.boundary(
			TokenRules.value('"', false),
			TokenRules.alwaysMatch(),
			TokenRules.value('"', false)
		), TokenActions.grouping(GroupingMode.MATCHED), false);
		
		this.builder.addRule(TokenRules.boundary(
			TokenRules.value('\'', false),
			TokenRules.alwaysMatch(),
			TokenRules.value('\'', false)
		), TokenActions.grouping(GroupingMode.MATCHED));
		//endregion
		
		this.builder.addRule(
			TokenRules.pattern("\\d+"),
			TokenActions.grouping(GroupingMode.MATCHED)
		);
		
		//region Comments
		TokenRule singleLineCommentRule = TokenRules.boundary(
			TokenRules.value('/', false).exactly(2),
			TokenRules.alwaysMatch(),
			TokenRules.any(
				TokenRules.lookahead(TokenRules.value('\n', false)),
				TokenRules.endDocument()
			)
		);
		TokenRule multiLineCommentRule = TokenRules.boundary(
			TokenRules.sequence(
				TokenRules.value('/', false).between(1, 2),
				TokenRules.value('*', false)
			),
			TokenRules.alwaysMatch(),
			TokenRules.sequence(
				TokenRules.value('*', false),
				TokenRules.value('/', false)
			)
		);
		
		this.builder.addRule(TokenRules.any(
			singleLineCommentRule,
			multiLineCommentRule
		), TokenActions.grouping(GroupingMode.MATCHED));
		//endregion
	}
	
	public void addRules() {
		this.builder.addRule(TokenRules.any(
			TokenRules.value(' ', false),
			TokenRules.value('\t', false)
		), TokenActions.convert(Token::shadow));
		
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value("package", false),
			TokenRules.reference("FullQualifiedName"),
			TokenRules.value(';', false)
		), TokenActions.grouping(GroupingMode.ALL));
		
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value("import", false),
			TokenRules.value("static", true).optional(),
			TokenRules.reference("FullQualifiedName"),
			TokenRules.sequence(
				TokenRules.value('.', false),
				TokenRules.value('*', false)
			).optional(),
			TokenRules.value(';', false)
		), TokenActions.grouping(GroupingMode.ALL));
		
		this.builder.addRule(TokenRules.sequence(
			TokenRules.value('@', false),
			TokenRules.all(
				TokenRules.reference("Identifier"),
				TokenRules.value("interface", false).not()
			),
			TokenRules.sequence(
				TokenRules.value('(', false),
				TokenRules.value(')', false)
			).optional()
		), TokenActions.grouping(GroupingMode.ALL));
	}
}
