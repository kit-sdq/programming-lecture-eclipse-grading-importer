package edu.kit.praktomat;

import java.util.List;
import java.util.stream.Collectors;

public final class PraktomatComments {
	private final String message;
	private final List<PraktomatComment> comments;

	public PraktomatComments(final String message, final List<PraktomatComment> comments) {
		this.message = message;
		this.comments = comments;
	}

	public PraktomatComments edit(final String newMessage) {
		final List<PraktomatComment> newComments = comments.stream().map(comment -> comment.edit(newMessage))
				.collect(Collectors.toList());
		return new PraktomatComments(newMessage, newComments);
	}

	public String getMessage() {
		return message;
	}

	public List<PraktomatComment> getComments() {
		return comments;
	}
}